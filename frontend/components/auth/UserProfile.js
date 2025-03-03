// components/auth/UserProfile.js
import { useState } from 'react';
import { getUserData, clearUserData } from '../../utils/auth';
import { logout } from '../../services/api';

export default function UserProfile({ onLogout }) {
    const [isLoggingOut, setIsLoggingOut] = useState(false);
    const userData = getUserData();

    if (!userData) {
        return null;
    }

    const handleLogout = async () => {
        try {
            setIsLoggingOut(true);

            // Call the backend to revoke the token
            await logout(userData.userId);

            // Clear local storage
            clearUserData();

            // Call the parent component's callback
            if (onLogout) {
                onLogout();
            }

            // Refresh the page
            window.location.reload();
        } catch (error) {
            console.error('Logout error:', error);
            // Even if the API call fails, clear the local data
            clearUserData();
            if (onLogout) {
                onLogout();
            }
            window.location.reload();
        }
    };

    return (
        <div className="user-profile">
            <div className="user-info">
                <h3>Welcome, Twitch User!</h3>
                <p>User ID: {userData.userId}</p>
            </div>

            <button
                onClick={handleLogout}
                disabled={isLoggingOut}
                className="logout-button"
            >
                {isLoggingOut ? 'Logging out...' : 'Logout'}
            </button>

            <style jsx>{`
        .user-profile {
          display: flex;
          align-items: center;
          justify-content: space-between;
          padding: 12px 20px;
          background-color: #f5f5f5;
          border-radius: 4px;
          margin-bottom: 20px;
        }
        
        .user-info {
          display: flex;
          flex-direction: column;
        }
        
        .user-info h3 {
          margin: 0;
          font-size: 16px;
        }
        
        .user-info p {
          margin: 5px 0 0;
          font-size: 14px;
          color: #666;
        }
        
        .logout-button {
          background-color: #d32f2f;
          color: white;
          border: none;
          padding: 8px 16px;
          border-radius: 4px;
          cursor: pointer;
          font-weight: bold;
          transition: background-color 0.2s;
        }
        
        .logout-button:hover {
          background-color: #b71c1c;
        }
        
        .logout-button:disabled {
          background-color: #e57373;
          cursor: not-allowed;
        }
      `}</style>
        </div>
    );
}