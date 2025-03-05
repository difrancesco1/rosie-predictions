// services/leagueApi.js
import { getUserData } from '../utils/auth';

const API_BASE_URL = 'http://localhost:8080/api';

/**
 * Connect a League of Legends account
 */
export async function connectLeagueAccount(summonerName) {
    try {
        const userData = getUserData();
        if (!userData || !userData.userId) {
            throw new Error('User is not authenticated');
        }

        const response = await fetch(`${API_BASE_URL}/league/${userData.userId}/connect`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ summonerName }),
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.error || 'Failed to connect League account');
        }

        return await response.json();
    } catch (error) {
        console.error('Error connecting League account:', error);
        throw error;
    }
}

/**
 * Get all connected League accounts
 */
export async function getAllLeagueAccounts() {
    try {
        const userData = getUserData();
        if (!userData || !userData.userId) {
            throw new Error('User is not authenticated');
        }

        const response = await fetch(`${API_BASE_URL}/league/${userData.userId}/accounts`);

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.error || 'Failed to get League accounts');
        }

        return await response.json();
    } catch (error) {
        console.error('Error getting League accounts:', error);
        throw error;
    }
}

/**
 * Get active League account
 */
export async function getActiveLeagueAccount() {
    try {
        const userData = getUserData();
        if (!userData || !userData.userId) {
            throw new Error('User is not authenticated');
        }

        const response = await fetch(`${API_BASE_URL}/league/${userData.userId}/active`);

        if (response.status === 404) {
            return null; // No active account
        }

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.error || 'Failed to get active League account');
        }

        return await response.json();
    } catch (error) {
        console.error('Error getting active League account:', error);
        throw error;
    }
}

/**
 * Set a League account as active
 */
export async function setAccountActive(accountId) {
    try {
        const userData = getUserData();
        if (!userData || !userData.userId) {
            throw new Error('User is not authenticated');
        }

        const response = await fetch(`${API_BASE_URL}/league/${userData.userId}/accounts/${accountId}/activate`, {
            method: 'PATCH',
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.error || 'Failed to set account as active');
        }

        return await response.json();
    } catch (error) {
        console.error('Error setting account as active:', error);
        throw error;
    }
}

/**
 * Update League account settings
 */
export async function updateLeagueSettings(accountId, settings) {
    try {
        const userData = getUserData();
        if (!userData || !userData.userId) {
            throw new Error('User is not authenticated');
        }

        const response = await fetch(`${API_BASE_URL}/league/${userData.userId}/accounts/${accountId}/settings`, {
            method: 'PATCH',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(settings),
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.error || 'Failed to update League settings');
        }

        return await response.json();
    } catch (error) {
        console.error('Error updating League settings:', error);
        throw error;
    }
}

/**
 * Disconnect League account
 */
export async function disconnectLeagueAccount(accountId) {
    try {
        const userData = getUserData();
        if (!userData || !userData.userId) {
            throw new Error('User is not authenticated');
        }

        const response = await fetch(`${API_BASE_URL}/league/${userData.userId}/accounts/${accountId}`, {
            method: 'DELETE',
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.error || 'Failed to disconnect League account');
        }

        return await response.json();
    } catch (error) {
        console.error('Error disconnecting League account:', error);
        throw error;
    }
}