// components/predictions/PredictionList.js
import { useState, useEffect } from 'react';
import { getPredictions } from '../../services/predictions.js';
import PredictionCard from './PredictionCard';

export default function PredictionList() {
    const [predictions, setPredictions] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState(null);
    const [filter, setFilter] = useState('all'); // 'all', 'active', 'resolved', 'canceled'

    const fetchPredictions = async () => {
        setIsLoading(true);
        setError(null);

        try {
            const data = await getPredictions(50); // Get last 50 predictions
            setPredictions(data);
        } catch (error) {
            setError(error.message || 'Failed to load predictions');
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        fetchPredictions();
    }, []);

    const filteredPredictions = predictions.filter(prediction => {
        if (filter === 'all') return true;
        return prediction.status.toLowerCase() === filter;
    });

    return (
        <div className="prediction-list">
            <div className="prediction-list-header">
                <h2>Predictions</h2>
                <div className="filter-controls">
                    <button
                        onClick={() => setFilter('all')}
                        className={filter === 'all' ? 'active' : ''}
                    >
                        All
                    </button>
                    <button
                        onClick={() => setFilter('active')}
                        className={filter === 'active' ? 'active' : ''}
                    >
                        Active
                    </button>
                    <button
                        onClick={() => setFilter('resolved')}
                        className={filter === 'resolved' ? 'active' : ''}
                    >
                        Resolved
                    </button>
                    <button
                        onClick={() => setFilter('canceled')}
                        className={filter === 'canceled' ? 'active' : ''}
                    >
                        Canceled
                    </button>
                </div>
            </div>

            {isLoading ? (
                <div className="loading">Loading predictions...</div>
            ) : error ? (
                <div className="error-message">{error}</div>
            ) : filteredPredictions.length === 0 ? (
                <div className="empty-state">
                    {filter === 'all'
                        ? 'No predictions found. Create your first prediction above!'
                        : `No ${filter} predictions found.`}
                </div>
            ) : (
                <div className="predictions-container">
                    {filteredPredictions.map(prediction => (
                        <PredictionCard
                            key={prediction.id}
                            prediction={prediction}
                            onUpdate={fetchPredictions}
                        />
                    ))}
                </div>
            )}

            <style jsx>{`
        .prediction-list {
          margin-top: 24px;
        }
        
        .prediction-list-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 16px;
        }
        
        .prediction-list-header h2 {
          margin: 0;
        }
        
        .filter-controls button {
          background: none;
          border: none;
          padding: 6px 12px;
          margin-left: 8px;
          border-radius: 4px;
          cursor: pointer;
        }
        
        .filter-controls button.active {
          background-color: #9146FF;
          color: white;
        }
        
        .loading, .empty-state {
          padding: 24px;
          text-align: center;
          background-color: #f5f5f5;
          border-radius: 8px;
        }
        
        .error-message {
          background-color: #ffebee;
          color: #d32f2f;
          padding: 16px;
          border-radius: 8px;
          margin-bottom: 16px;
        }
      `}</style>
        </div>
    );
}