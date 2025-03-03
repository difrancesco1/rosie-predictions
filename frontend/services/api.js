// services/api.js
const API_BASE_URL = 'http://localhost:8080/api';

/**
 * Get the Twitch authorization URL
 */
export async function getTwitchAuthUrl() {
    try {
        const response = await fetch(`${API_BASE_URL}/auth/twitch/url`);

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.error || 'Failed to get auth URL');
        }

        const data = await response.json();
        return data.url;
    } catch (error) {
        console.error('Error getting Twitch auth URL:', error);
        throw error;
    }
}

/**
 * Handle the Twitch auth callback code
 */
export async function handleTwitchCallback(code) {
    try {
        console.log('Sending auth code to backend:', code);

        // Send code as a query parameter which is what the backend expects
        const response = await fetch(`${API_BASE_URL}/auth/twitch/callback?code=${encodeURIComponent(code)}`);

        console.log('Response status:', response.status);

        const responseText = await response.text();
        console.log('Response body:', responseText);

        if (!response.ok) {
            throw new Error(`Failed to authenticate with Twitch: ${responseText}`);
        }

        // Parse the JSON response after checking it's valid
        let data;
        try {
            data = JSON.parse(responseText);
        } catch (e) {
            console.error('Error parsing JSON response:', e);
            throw new Error('Invalid response from server');
        }

        return data;
    } catch (error) {
        console.error('Error handling Twitch callback:', error);
        throw error;
    }
}

/**
 * Check authentication status
 */
export async function checkAuthStatus(userId) {
    try {
        const response = await fetch(`${API_BASE_URL}/auth/twitch/status/${userId}`);

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.error || 'Failed to check auth status');
        }

        return await response.json();
    } catch (error) {
        console.error('Error checking auth status:', error);
        throw error;
    }
}

/**
 * Logout the user
 */
export async function logout(userId) {
    try {
        const response = await fetch(`${API_BASE_URL}/auth/twitch/logout/${userId}`, {
            method: 'POST',
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.error || 'Failed to logout');
        }

        return await response.json();
    } catch (error) {
        console.error('Error logging out:', error);
        throw error;
    }
}

// create prediction

export async function createPrediction(data) {
    try {
        const response = await fetch('/api/predictions', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(data),
        });
        if (!response.ok) {
            throw new Error('Failed to create prediction');
        }
        return await response.json();
    } catch (error) {
        console.error('Error creating prediction:', error);
        throw error;
    }
}

// get Active predictions
export async function getActivePredictions() {
    try {
        const response = await fetch('/api/predictions/active');
        if (!response.ok) {
            throw new Error('Failed to fetch active predictions');
        }
        return await response.json();
    } catch (error) {
        console.error('Error fetching active predictions:', error);
        throw error;
    }
}


export async function resolvePrediction(predictionId) {
    try {
        const response = await fetch(`/api/predictions/${predictionId}/resolve`, {
            method: 'POST',
        });
        if (!response.ok) {
            throw new Error('Failed to resolve prediction');
        }
        return await response.json();
    } catch (error) {
        console.error('Error resolving prediction:', error);
        throw error;
    }
}

export async function cancelPrediction(predictionId) {
    try {
        const response = await fetch(`/api/predictions/${predictionId}/cancel`, {
            method: 'POST',
        });
        if (!response.ok) {
            throw new Error('Failed to cancel prediction');
        }
        return await response.json();
    } catch (error) {
        console.error('Error canceling prediction:', error);
        throw error;
    }
}