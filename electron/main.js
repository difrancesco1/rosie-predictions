const { app, BrowserWindow, ipcMain, shell, globalShortcut } = require('electron');
const path = require('path');
const { spawn } = require('child_process');
const http = require('http');
const https = require('https');

// Set environment variables
const isDev = process.env.NODE_ENV !== 'production';
const PORT = process.env.PORT || 3000;
let mainWindow;
let springProcess;

function createWindow() {
    mainWindow = new BrowserWindow({
        width: 363,
        height: 649,
        resizable: false, // Prevent window resizing
        webPreferences: {
            nodeIntegration: false,
            contextIsolation: true,
            preload: path.join(__dirname, 'preload.js'),
        }
    });

    // In development mode, connect to the Next.js dev server
    // In production, load from the static files
    const startURL = isDev
        ? `http://localhost:${PORT}`
        : `file://${path.join(__dirname, '../frontend/out/index.html')}`;
    console.log(`Loading URL: ${startURL}`);
    mainWindow.loadURL(startURL);

    if (isDev) {
        // Open DevTools in a separate window
        mainWindow.webContents.openDevTools({ mode: 'detach' });
    }

    mainWindow.on('closed', () => {
        mainWindow = null;
    });

    // Handle external URLs - important for OAuth
    mainWindow.webContents.setWindowOpenHandler(({ url }) => {
        // Open OAuth and external links in the user's default browser
        if (url.startsWith('https://id.twitch.tv/') ||
            (!url.startsWith('http://localhost') &&
                !url.startsWith('file://'))) {
            shell.openExternal(url);
            return { action: 'deny' };
        }
        return { action: 'allow' };
    });
}

// For development, we're not starting Spring Boot
// since it's being started by the concurrently command
function startSpringBootApp() {
    if (!isDev) {
        // In production, start the JAR file
        const jarPath = path.join(process.resourcesPath, 'app/backend.jar');
        console.log('Starting Spring Boot app from:', jarPath);
        springProcess = spawn('java', ['-jar', jarPath]);
        springProcess.stdout.on('data', (data) => {
            console.log(`Spring Boot: ${data}`);
        });
        springProcess.stderr.on('data', (data) => {
            console.error(`Spring Boot Error: ${data}`);
        });
        springProcess.on('close', (code) => {
            console.log(`Spring Boot process exited with code ${code}`);
        });
    }
}

app.on('ready', () => {
    if (isDev) {
        // In development, just create the window
        // (Spring Boot and Next.js are started by concurrently)
        createWindow();
    } else {
        // In production, start Spring Boot first, then create the window
        startSpringBootApp();
        setTimeout(createWindow, 3000);
    }
    // Register keyboard shortcut for toggling DevTools
    globalShortcut.register('CommandOrControl+Shift+I', () => {
        if (mainWindow) {
            mainWindow.webContents.toggleDevTools({ mode: 'detach' });
        }
    });
});

app.on('window-all-closed', () => {
    if (process.platform !== 'darwin') {
        app.quit();
    }
});

app.on('will-quit', () => {
    // Unregister all shortcuts
    globalShortcut.unregisterAll();
});

app.on('quit', () => {
    if (springProcess) {
        // Force kill the process if we started it
        if (process.platform === 'win32') {
            spawn('taskkill', ['/pid', springProcess.pid, '/f', '/t']);
        } else {
            process.kill(-springProcess.pid);
        }
    }
});

app.on('activate', () => {
    if (mainWindow === null) {
        createWindow();
    }
});

// Helper function to make HTTP requests
function makeRequest(url) {
    return new Promise((resolve, reject) => {
        const client = url.startsWith('https') ? https : http;
        const req = client.get(url, (res) => {
            if (res.statusCode < 200 || res.statusCode >= 300) {
                return reject(new Error(`Status Code: ${res.statusCode}`));
            }

            const data = [];
            res.on('data', (chunk) => data.push(chunk));
            res.on('end', () => {
                try {
                    const body = Buffer.concat(data).toString();
                    resolve(JSON.parse(body));
                } catch (e) {
                    reject(e);
                }
            });
        });

        req.on('error', (err) => reject(err));
        req.end();
    });
}

// IPC handlers for authentication and navigation
ipcMain.on('open-external', (event, { url }) => {
    shell.openExternal(url);
});

ipcMain.on('navigate', (event, { path }) => {
    const baseUrl = isDev
        ? `http://localhost:${PORT}`
        : `file://${path.join(__dirname, '../frontend/out/index.html')}`;
    mainWindow.loadURL(`${baseUrl}${path}`);
});

ipcMain.on('handle-twitch-auth', async (event, { url }) => {
    try {
        // Get main window bounds
        const mainBounds = mainWindow.getBounds();

        // Calculate position for the auth window to be to the left of the main window
        const authWidth = 363;
        const authHeight = 649;
        const authX = Math.max(0, mainBounds.x - authWidth - 10); // 10px gap
        const authY = mainBounds.y; // Align with main window's y position

        // Create auth window with adjusted position
        const authWindow = new BrowserWindow({
            width: authWidth,
            height: authHeight,
            x: authX,
            y: authY,
            parent: mainWindow,
            resizable: false,
            modal: true,
            show: false,
            webPreferences: {
                nodeIntegration: false,
                contextIsolation: true
            }
        });

        authWindow.setMenuBarVisibility(false);

        // Show window when ready
        authWindow.once('ready-to-show', () => {
            authWindow.show();
        });

        // Track if we've handled the auth
        let authHandled = false;

        // Monitor URL changes to detect the callback
        const checkForCallback = async (newUrl) => {
            if (newUrl.includes('callback') && newUrl.includes('code=') && !authHandled) {
                authHandled = true;

                // Parse the code from the URL
                const urlObj = new URL(newUrl);
                const code = urlObj.searchParams.get('code');

                if (code) {
                    try {
                        // Process auth via our API
                        const apiUrl = `http://localhost:8080/api/auth/twitch/callback?code=${encodeURIComponent(code)}`;
                        const userData = await makeRequest(apiUrl);

                        // Close auth window after we've processed the code
                        authWindow.destroy();

                        // Send success back to renderer
                        event.sender.send('auth-result', {
                            success: true,
                            userData
                        });
                    } catch (error) {
                        console.error('Auth processing error:', error);
                        authWindow.destroy();
                        event.sender.send('auth-result', {
                            success: false,
                            error: error.message || 'Authentication failed'
                        });
                    }
                }
                return true;
            } else if (newUrl.includes('error=') && !authHandled) {
                authHandled = true;

                // Handle auth error
                const urlObj = new URL(newUrl);
                const error = urlObj.searchParams.get('error');

                authWindow.destroy();
                event.sender.send('auth-result', {
                    success: false,
                    error: error || 'Authentication error'
                });
                return true;
            }
            return false;
        };

        // Use both these events to catch the redirect
        authWindow.webContents.on('will-redirect', async (e, newUrl) => {
            await checkForCallback(newUrl);
        });

        authWindow.webContents.on('did-navigate', async (e, newUrl) => {
            await checkForCallback(newUrl);
        });

        // Handle window close
        authWindow.on('closed', () => {
            if (!authHandled) {
                event.sender.send('auth-result', {
                    success: false,
                    error: 'Authentication cancelled'
                });
            }
        });

        // Load the Twitch auth URL
        authWindow.loadURL(url);

    } catch (error) {
        console.error('Failed to create auth window:', error);
        event.sender.send('auth-result', {
            success: false,
            error: error.message || 'Authentication failed'
        });
    }
});
