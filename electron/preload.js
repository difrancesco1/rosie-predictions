const { contextBridge, ipcRenderer } = require('electron');

// Expose protected methods that allow the renderer process to use
// the ipcRenderer without exposing the entire object
contextBridge.exposeInMainWorld('electron', {
    openExternal: (url) => {
        ipcRenderer.send('open-external', { url });
    },
    navigate: (path) => {
        ipcRenderer.send('navigate', { path });
    },
    // New methods for Twitch auth
    handleTwitchAuth: (url) => {
        ipcRenderer.send('handle-twitch-auth', { url });
    },
    onAuthResult: (callback) => {
        // Remove existing listeners to prevent duplicates
        ipcRenderer.removeAllListeners('auth-result');
        // Add new listener
        ipcRenderer.on('auth-result', (event, data) => callback(data));
    }
});