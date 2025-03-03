// utils/auth.js

// Store user data in local storage
export function setUserData(userData) {
    if (typeof window !== 'undefined') {
        localStorage.setItem('twitchUser', JSON.stringify(userData));
    }
}

// Get user data from local storage
export function getUserData() {
    if (typeof window !== 'undefined') {
        try {
            const userData = localStorage.getItem('twitchUser');
            return userData ? JSON.parse(userData) : null;
        } catch (error) {
            console.error('Error parsing user data from localStorage:', error);
            return null;
        }
    }
    return null;
}

// Remove user data from local storage
export function clearUserData() {
    if (typeof window !== 'undefined') {
        localStorage.removeItem('twitchUser');
    }
}

// Check if the user is authenticated
export function isAuthenticated() {
    return !!getUserData();
}