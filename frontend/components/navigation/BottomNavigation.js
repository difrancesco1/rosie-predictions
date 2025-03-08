import React from 'react';
import styles from '../../styles/BottomNavigation.module.css';

const BottomNavigation = ({ onNavigate }) => {
    return (
        <div className={styles.bottomNavContainer}>
            <button
                className={styles.navButton}
                onClick={() => onNavigate('create')}
            >
                Create
            </button>
            <button
                className={styles.navButton}
                onClick={() => onNavigate('list')}
            >
                Predictions
            </button>
            <button
                className={styles.navButton}
                onClick={() => onNavigate('league')}
            >
                League
            </button>
        </div>
    );
};

export default BottomNavigation;