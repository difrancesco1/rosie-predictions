// components/predictions/PredictionCard.js
import { useState } from 'react';
import { resolvePrediction, cancelPrediction } from '../../services/predictions.js';

export default function PredictionCard({ prediction, onUpdate }) {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);
  const [selectedOutcome, setSelectedOutcome] = useState(null);
  const [showActions, setShowActions] = useState(false);

  const isActive = prediction.status === 'ACTIVE';
  const isResolved = prediction.status === 'RESOLVED';
  const isCanceled = prediction.status === 'CANCELED';

  const handleResolve = async () => {
    if (!selectedOutcome) {
      setError('Please select a winning outcome');
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      await resolvePrediction(prediction.id, selectedOutcome);
      if (onUpdate) onUpdate();
    } catch (error) {
      setError(error.message || 'Failed to resolve prediction');
    } finally {
      setIsLoading(false);
    }
  };

  const handleCancel = async () => {
    setIsLoading(true);
    setError(null);

    try {
      await cancelPrediction(prediction.id);
      if (onUpdate) onUpdate();
    } catch (error) {
      setError(error.message || 'Failed to cancel prediction');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className={`prediction-card ${prediction.status.toLowerCase()}`}>
      <div className="prediction-header">
        <h3>{prediction.title}</h3>
        <div className="status-badge">{prediction.status}</div>
      </div>

      <div className="prediction-outcomes">
        {prediction.outcomes.map(outcome => (
          <div
            key={outcome.id}
            className={`outcome ${isResolved && prediction.winningOutcomeId === outcome.id ? 'winner' : ''
              } ${selectedOutcome === outcome.id ? 'selected' : ''}`}
            onClick={() => {
              if (isActive) setSelectedOutcome(outcome.id);
            }}
          >
            <div className="outcome-title">{outcome.title}</div>
            {(isResolved || isCanceled) && (
              <div className="outcome-stats">
                <span>{outcome.users} users</span>
                <span>{outcome.channelPoints.toLocaleString()} points</span>
              </div>
            )}
          </div>
        ))}
      </div>

      {error && <div className="error-message">{error}</div>}

      <div className="prediction-footer">
        <div className="prediction-details">
          <div>Created: {new Date(prediction.createdAt).toLocaleString()}</div>
          {prediction.endedAt && (
            <div>Ended: {new Date(prediction.endedAt).toLocaleString()}</div>
          )}
        </div>

        {isActive && (
          <div className="prediction-actions">
            <button
              className="action-button"
              onClick={() => setShowActions(!showActions)}
            >
              Actions
            </button>

            {showActions && (
              <div className="action-dropdown">
                <button
                  onClick={handleResolve}
                  disabled={isLoading || !selectedOutcome}
                  className="resolve-button"
                >
                  {isLoading ? 'Processing...' : 'Resolve'}
                </button>
                <button
                  onClick={handleCancel}
                  disabled={isLoading}
                  className="cancel-button"
                >
                  {isLoading ? 'Processing...' : 'Cancel'}
                </button>
              </div>
            )}
          </div>
        )}
      </div>

      <style jsx>{`
        .prediction-card {
          background-color: white;
          border-radius: 8px;
          padding: 16px;
          margin-bottom: 16px;
          box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
          border-left: 4px solid #9146FF;
        }
        
        .prediction-card.resolved {
          border-left-color: #4CAF50;
        }
        
        .prediction-card.canceled {
          border-left-color: #F44336;
        }
        
        .prediction-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 12px;
        }
        
        .prediction-header h3 {
          margin: 0;
          font-size: 18px;
        }
        
        .status-badge {
          font-size: 12px;
          padding: 4px 8px;
          border-radius: 4px;
          background-color: #9146FF;
          color: white;
        }
        
        .prediction-card.resolved .status-badge {
          background-color: #4CAF50;
        }
        
        .prediction-card.canceled .status-badge {
          background-color: #F44336;
        }
        
        .prediction-outcomes {
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: 8px;
          margin-bottom: 16px;
        }
        
        .outcome {
          padding: 12px;
          border-radius: 4px;
          background-color: #f5f5f5;
          cursor: pointer;
          border: 2px solid transparent;
        }
        
        .outcome.selected {
          border-color: #9146FF;
        }
        
        .outcome.winner {
          background-color: #E8F5E9;
          border-color: #4CAF50;
        }
        
        .outcome-title {
          font-weight: bold;
          margin-bottom: 4px;
        }
        
        .outcome-stats {
          display: flex;
          justify-content: space-between;
          font-size: 12px;
          color: #666;
        }
        
        .error-message {
          background-color: #ffebee;
          color: #d32f2f;
          padding: 8px;
          border-radius: 4px;
          margin-bottom: 12px;
        }
        
        .prediction-footer {
          display: flex;
          justify-content: space-between;
          align-items: center;
          font-size: 12px;
          color: #666;
        }
        
        .prediction-actions {
          position: relative;
        }
        
        .action-button {
          background-color: #9146FF;
          color: white;
          border: none;
          padding: 6px 12px;
          border-radius: 4px;
          cursor: pointer;
        }
        
        .action-dropdown {
          position: absolute;
          right: 0;
          bottom: 30px;
          background-color: white;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
          border-radius: 4px;
          width: 120px;
          z-index: 10;
        }
        
        .action-dropdown button {
          width: 100%;
          text-align: left;
          padding: 8px 12px;
          border: none;
          background: none;
          cursor: pointer;
        }
        
        .resolve-button:hover {
          background-color: #E8F5E9;
        }
        
        .cancel-button:hover {
          background-color: #FFEBEE;
        }
        
        .action-dropdown button:disabled {
          color: #ccc;
          cursor: not-allowed;
        }
      `}</style>
    </div>
  );
}