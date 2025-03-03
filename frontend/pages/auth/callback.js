// pages/auth/callback.js
import { useEffect, useState } from 'react';
import { useRouter } from 'next/router';
import { handleTwitchCallback } from '../../services/api';
import { setUserData } from '../../utils/auth';

export default function AuthCallback() {
    const router = useRouter();
    const [status, setStatus] = useState('Waiting for authentication data...');
    const [error, setError] = useState(null);

    useEffect(() => {
        // Function to process the authentication code
        const processAuth = async (code) => {
            try {
                setStatus('Finalizing authentication...');
                console.log('Processing auth code:', code);

                // Send the code to our backend
                const userData = await handleTwitchCallback(code);
                console.log('Received user data:', userData);

                // Save the user data
                setUserData(userData);

                setStatus('Authentication successful! Redirecting...');

                // Redirect to the main page
                setTimeout(() => {
                    router.push('/');
                }, 1500);
            } catch (error) {
                console.error('Auth callback error:', error);
                setError('Authentication failed. Please try again.');
                setStatus('');
            }
        };

        // Process once the router is ready and we have the code
        if (router.isReady && router.query.code) {
            processAuth(router.query.code);
        } else if (router.isReady && router.query.error) {
            setError(`Authentication error: ${router.query.error}`);
            setStatus('');
        }
    }, [router.isReady, router.query]);

    return (
        <div className="auth-callback-container">
            <h1>Twitch Authentication</h1>
            {status && <div className="status-message">{status}</div>}
            {error && <div className="error-message">{error}</div>}

            <style jsx>{`
        .auth-callback-container {
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          min-height: 100vh;
          padding: 20px;
          text-align: center;
        }
        
        .status-message {
          margin-top: 20px;
          padding: 10px;
          border-radius: 4px;
          background-color: #f5f5f5;
          max-width: 400px;
        }
        
        .error-message {
          margin-top: 20px;
          padding: 10px;
          border-radius: 4px;
          background-color: #ffebee;
          color: #d32f2f;
          max-width: 400px;
        }
      `}</style>
        </div>
    );
}