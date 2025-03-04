import { useState, useEffect } from 'react';
import { getTwitchAuthUrl } from '../../services/api';
import { setUserData } from '../../utils/auth';

export default function LoginButton({ setLoading, setLoadingMessage }) {
  const [error, setError] = useState(null);

  // Set up listener for auth results
  useEffect(() => {
    if (typeof window !== 'undefined' && window.electron) {
      window.electron.onAuthResult((result) => {
        if (result.success && result.userData) {
          // Auth successful
          setLoadingMessage("Authentication successful! Loading your predictions...");

          // Save the user data
          setUserData(result.userData);

          // Refresh the page to show authenticated content
          setTimeout(() => {
            window.location.reload();
          }, 1000);
        } else {
          // Auth failed
          setLoading(false);
          if (result.error) {
            setError(`Authentication failed: ${result.error}`);
          }
        }
      });
    }
  }, [setLoading, setLoadingMessage]);

  const handleLogin = async () => {
    try {
      // Show loading with initial message
      setLoading(true);
      setLoadingMessage("Connecting to Twitch...");
      setError(null);

      // Get the auth URL from the backend
      const authUrl = await getTwitchAuthUrl();

      // Check if we're in an Electron environment
      if (typeof window !== 'undefined' && window.electron) {
        // Use our new Electron handler for Twitch auth
        window.electron.handleTwitchAuth(authUrl);
      } else {
        // Fallback for regular browser environment
        window.location.href = authUrl;
      }
    } catch (error) {
      console.error('Login error:', error);
      setError('Failed to connect to Twitch. Please try again.');
      setLoading(false);
    }
  };

  return (
    <div className="twitch-login-container">
      <button
        onClick={handleLogin}
        className="twitch-login-button"
      >
        Login with Twitch
      </button>
      {error && <p className="twitch-error-message">{error}</p>}
    </div>
  );
}