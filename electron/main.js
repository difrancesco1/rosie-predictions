const { app, BrowserWindow, ipcMain, shell } = require('electron');
const path = require('path');
const { spawn } = require('child_process');
const isDev = process.env.NODE_ENV === 'development';
const PORT = process.env.PORT || 3000;

let mainWindow;
let springProcess;

function createWindow() {
    mainWindow = new BrowserWindow({
        width: 363,
        height: 649,
        // frame: false, // Removes default title bar
        // transparent: true, // Enables transparency
        // hasShadow: false, // Removes unwanted shadows
        webPreferences: {
            nodeIntegration: false,
            contextIsolation: true,
            preload: path.join(__dirname, 'preload.js'),
            // backgroundThrottling: false // Ensures rendering even when unfocused
        }
    });

    const startURL = isDev
        ? `http://localhost:${PORT}`
        : `file://${path.join(__dirname, '../frontend/out/index.html')}`;

    mainWindow.loadURL(startURL);

    if (isDev) {
        mainWindow.webContents.openDevTools();
    }

    mainWindow.on('closed', () => {
        mainWindow = null;
    });

    // Handle external URLs - important for OAuth
    mainWindow.webContents.setWindowOpenHandler(({ url }) => {
        // Open OAuth and external links in the user's default browser
        if (url.startsWith('https://id.twitch.tv/') ||
            !url.startsWith('http://localhost') &&
            !url.startsWith('file://')) {
            shell.openExternal(url);
            return { action: 'deny' };
        }
        return { action: 'allow' };
    });
}

function startSpringBootApp() {
    const jarPath = isDev
        ? path.join(__dirname, '../backend/target/backend-0.0.1-SNAPSHOT.jar')
        : path.join(process.resourcesPath, 'app/backend.jar');

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

app.on('ready', () => {
    startSpringBootApp();
    setTimeout(createWindow, 3000); // Give Spring Boot time to start
});

app.on('window-all-closed', () => {
    if (process.platform !== 'darwin') {
        app.quit();
    }
});

app.on('quit', () => {
    if (springProcess) {
        process.kill(springProcess.pid);
    }
});

app.on('activate', () => {
    if (mainWindow === null) {
        createWindow();
    }
});

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