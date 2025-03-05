// components/MainContainer.js
import { useState } from 'react';
import CreatePredictionForm from './predictions/CreatePredictionForm';
import PredictionList from './predictions/PredictionList';
import LeagueIntegration from './integrations/LeagueIntegration';
import styles from './MainContainer.module.css';

export default function MainContainer() {
    const [activeTab, setActiveTab] = useState('create'); // 'create', 'list', 'league'

    // Function to handle prediction creation success
    const handlePredictionCreated = () => {
        // Optionally switch to prediction list after creation
        // setActiveTab('list');

        // Or just trigger a refresh of the prediction list if that component has a refresh method
    };

    return (
        <div className={styles.container}>
            <div className={styles.tabButtons}>
                <button
                    className={`${styles.tabButton} ${activeTab === 'create' ? styles.activeTab : ''}`}
                    onClick={() => setActiveTab('create')}
                >
                    Create
                </button>
                <button
                    className={`${styles.tabButton} ${activeTab === 'list' ? styles.activeTab : ''}`}
                    onClick={() => setActiveTab('list')}
                >
                    Predictions
                </button>
                <button
                    className={`${styles.tabButton} ${activeTab === 'league' ? styles.activeTab : ''}`}
                    onClick={() => setActiveTab('league')}
                >
                    League
                </button>
            </div>

            <div className={styles.contentContainer}>
                {activeTab === 'create' && (
                    <CreatePredictionForm onSuccess={handlePredictionCreated} />
                )}

                {activeTab === 'list' && (
                    <PredictionList />
                )}

                {activeTab === 'league' && (
                    <LeagueIntegration />
                )}
            </div>
        </div>
    );
}