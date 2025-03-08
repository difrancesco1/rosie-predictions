// components/MockToggle.js
import { useState, useEffect } from 'react';
import styles from './MockToggle.module.css';

/**
 * A UI component that shows when the application is running in mock mode
 * and provides information about the simulated environment
 */
export default function MockToggle() {
    const [isMockMode, setIsMockMode] = useState(false);
    const [showDetails, setShowDetails] = useState(false);

    useEffect(() => {
        // Check if running in mock mode
        const checkMockMode = async () => {
            try {
                const response = await fetch('http://localhost:8080/api/status');
                const data = await response.json();

                // The API controller should return a "mockMode" flag if true
                setIsMockMode(data.mockMode === true);
            } catch (error) {
                console.error('Error checking mock mode:', error);
                setIsMockMode(false);
            }
        };

        checkMockMode();
    }, []);

    // Don't render anything if not in mock mode
    if (!isMockMode) {
        return null;
    }

    return (
        <div className={styles.mockToggle}>
            <div
                className={styles.mockBadge}
                onClick={() => setShowDetails(!showDetails)}
            >
                MOCK MODE
            </div>

            {showDetails && (
                <div className={styles.mockDetails}>
                    <h4>Mock Mode Active</h4>
                    <p>The application is running in simulation mode.</p>
                    <ul>
                        <li>Predictions are simulated and not broadcast to Twitch</li>
                        <li>Authentication is automatic with a mock user</li>
                        <li>Data is stored in a separate test database</li>
                        <li>No live stream is required</li>
                    </ul>
                    <p className={styles.note}>Perfect for testing and development</p>
                </div>
            )}
        </div>
    );
}