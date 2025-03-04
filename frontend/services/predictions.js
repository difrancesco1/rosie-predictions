// services/predictionApi.js
import { getUserData } from '../utils/auth';

const API_BASE_URL = 'http://localhost:8080/api';

/**
 * Get recent predictions
 */
export async function getPredictions(limit = 20) {
    try {
        const userData = getUserData();
        if (!userData) throw new Error('User not authenticated');

        const response = await fetch(`${API_BASE_URL}/predictions/${userData.userId}?limit=${limit}`);

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.error || 'Failed to fetch predictions');
        }

        return await response.json();
    } catch (error) {
        console.error('Error fetching predictions:', error);
        throw error;
    }
}

/**
 * Create a new prediction
 */
export async function createPrediction(data) {
    try {
        const userData = getUserData();
        if (!userData) throw new Error('User not authenticated');

        const response = await fetch(`${API_BASE_URL}/predictions/${userData.userId}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(data),
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.error || 'Failed to create prediction');
        }

        return await response.json();
    } catch (error) {
        console.error('Error creating prediction:', error);
        throw error;
    }
}

/**
 * Resolve a prediction
 */
export async function resolvePrediction(predictionId, winningOutcomeId) {
    try {
        const userData = getUserData();
        if (!userData) throw new Error('User not authenticated');

        const response = await fetch(`${API_BASE_URL}/predictions/${userData.userId}/${predictionId}/resolve`, {
            method: 'PATCH',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ winningOutcomeId }),
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.error || 'Failed to resolve prediction');
        }

        return await response.json();
    } catch (error) {
        console.error('Error resolving prediction:', error);
        throw error;
    }
}

/**
 * Cancel a prediction
 */
export async function cancelPrediction(predictionId) {
    try {
        const userData = getUserData();
        if (!userData) throw new Error('User not authenticated');

        const response = await fetch(`${API_BASE_URL}/predictions/${userData.userId}/${predictionId}/cancel`, {
            method: 'PATCH',
            headers: {
                'Content-Type': 'application/json',
            },
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.error || 'Failed to cancel prediction');
        }

        return await response.json();
    } catch (error) {
        console.error('Error canceling prediction:', error);
        throw error;
    }
}