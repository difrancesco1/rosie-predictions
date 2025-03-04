import { useState } from 'react';
import { getUserData, clearUserData } from '../../utils/auth';
import { logout } from '../../services/api';
import styles from './UserProfile.module.css';

export default function UserProfile({ onLogout }) {
  const [isExpanded, setIsExpanded] = useState(false);
  const [isLoggingOut, setIsLoggingOut] = useState(false);
  const userData = getUserData();

  if (!userData) {
    return null;
  }

  const handleLogout = async () => {
    try {
      setIsLoggingOut(true);
      await logout(userData.userId);
      clearUserData();

      if (onLogout) {
        onLogout();
      }

      window.location.reload();
    } catch (error) {
      console.error('Logout error:', error);
      clearUserData();
      if (onLogout) {
        onLogout();
      }
      window.location.reload();
    }
  };

  return (
    <div className={styles.userProfile}>
      <button
        className={styles.profileButton}
        onClick={() => setIsExpanded(!isExpanded)}
      >
        ☰
      </button>

      <div className={`${styles.profileContent} ${isExpanded ? styles.expanded : ''}`}>
        <div className={styles.userInfo}>
          <h3>Welcome, {userData.userId}</h3>
        </div>

        <button
          onClick={handleLogout}
          disabled={isLoggingOut}
          className={styles.logoutButton}
        >
          {isLoggingOut ? 'Logging out...' : 'Logout'}
        </button>
      </div>
    </div>
  );
}
