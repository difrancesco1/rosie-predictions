// components/auth/LoginButton.js
import { useState } from 'react';
import { getTwitchAuthUrl } from '../../services/api';

export default function LoginButton() {
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState(null);

    const handleLogin = async () => {
        try {
            setIsLoading(true);
            setError(null);

            // Get the auth URL from the backend
            const authUrl = await getTwitchAuthUrl();

            // Check if we're in an Electron environment
            if (typeof window !== 'undefined' && window.api) {
                // Use Electron's API to open the auth URL in an external browser
                window.api.send('open-external', { url: authUrl });
            } else {
                // Fallback for regular browser environment
                window.location.href = authUrl;
            }
        } catch (error) {
            console.error('Login error:', error);
            setError('Failed to connect to Twitch. Please try again.');
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="login-container">
            <button
                onClick={handleLogin}
                disabled={isLoading}
                className="login-button"
            >
                {isLoading ? 'Connecting...' : 'Login with Twitch'}
            </button>

            {error && <p className="error-message">{error}</p>}

            <style jsx>{`
        .login-container {
          display: flex;
          flex-direction: column;
          align-items: center;
          margin: 20px 0;
        }
        
        .login-button {
          background-color: #9146FF;
          color: white;
          border: none;
          padding: 12px 24px;
          font-size: 16px;
          border-radius: 4px;
          cursor: pointer;
          font-weight: bold;
          transition: background-color 0.2s;
        }
        
        .login-button:hover {
          background-color: #7d2ff3;
        }
        
        .login-button:disabled {
          background-color: #ccc;
          cursor: not-allowed;
        }
        
        .error-message {
          color: #d32f2f;
          margin-top: 10px;
        }
      `}</style>
        </div>
    );
}