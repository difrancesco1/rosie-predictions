// components/predictions/CreatePredictionForm.js
import { useState } from 'react';
import { createPrediction } from '../../services/predictions.js';

export default function CreatePredictionForm({ onSuccess }) {
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState(null);
    const [title, setTitle] = useState('');
    const [outcomes, setOutcomes] = useState(['', '']);
    const [predictionWindow, setPredictionWindow] = useState(60);
    const [sessionId, setSessionId] = useState('');

    const handleOutcomeChange = (index, value) => {
        const newOutcomes = [...outcomes];
        newOutcomes[index] = value;
        setOutcomes(newOutcomes);
    };

    const addOutcome = () => {
        if (outcomes.length < 10) {
            setOutcomes([...outcomes, '']);
        }
    };

    const removeOutcome = (index) => {
        if (outcomes.length > 2) {
            const newOutcomes = [...outcomes];
            newOutcomes.splice(index, 1);
            setOutcomes(newOutcomes);
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        // Validate form
        if (!title.trim()) {
            setError('Please enter a title for your prediction');
            return;
        }

        const filteredOutcomes = outcomes.filter(o => o.trim() !== '');
        if (filteredOutcomes.length < 2) {
            setError('You need at least two outcomes');
            return;
        }

        setIsLoading(true);
        setError(null);

        try {
            const data = {
                title,
                outcomes: filteredOutcomes,
                predictionWindow,
                sessionId: sessionId || null,
            };

            await createPrediction(data);

            // Reset form
            setTitle('');
            setOutcomes(['', '']);
            setPredictionWindow(60);

            if (onSuccess) {
                onSuccess();
            }
        } catch (error) {
            setError(error.message || 'Failed to create prediction');
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="create-prediction-form">
            <h2>Create New Prediction</h2>

            {error && <div className="error-message">{error}</div>}

            <form onSubmit={handleSubmit}>
                <div className="form-group">
                    <label htmlFor="prediction-title">Question</label>
                    <input
                        id="prediction-title"
                        type="text"
                        value={title}
                        onChange={(e) => setTitle(e.target.value)}
                        placeholder="What will happen next?"
                        disabled={isLoading}
                        required
                    />
                </div>

                <div className="form-group">
                    <label>Outcomes (2-10)</label>
                    {outcomes.map((outcome, index) => (
                        <div key={index} className="outcome-input">
                            <input
                                type="text"
                                value={outcome}
                                onChange={(e) => handleOutcomeChange(index, e.target.value)}
                                placeholder={`Outcome ${index + 1}`}
                                disabled={isLoading}
                                required
                            />
                            {outcomes.length > 2 && (
                                <button
                                    type="button"
                                    onClick={() => removeOutcome(index)}
                                    disabled={isLoading}
                                    className="remove-button"
                                >
                                    &times;
                                </button>
                            )}
                        </div>
                    ))}

                    {outcomes.length < 10 && (
                        <button
                            type="button"
                            onClick={addOutcome}
                            disabled={isLoading}
                            className="add-outcome-button"
                        >
                            + Add another outcome
                        </button>
                    )}
                </div>

                <div className="form-group">
                    <label htmlFor="prediction-window">Duration (seconds)</label>
                    <input
                        id="prediction-window"
                        type="number"
                        min="30"
                        max="1800"
                        value={predictionWindow}
                        onChange={(e) => setPredictionWindow(parseInt(e.target.value))}
                        disabled={isLoading}
                    />
                </div>

                <button
                    type="submit"
                    disabled={isLoading}
                    className="submit-button"
                >
                    {isLoading ? 'Creating...' : 'Create Prediction'}
                </button>
            </form>

            <style jsx>{`
        .create-prediction-form {
          background-color: #f5f5f5;
          padding: 20px;
          border-radius: 8px;
          margin-bottom: 20px;
        }
        
        .error-message {
          background-color: #ffebee;
          color: #d32f2f;
          padding: 10px;
          border-radius: 4px;
          margin-bottom: 15px;
        }
        
        .form-group {
          margin-bottom: 15px;
        }
        
        label {
          display: block;
          margin-bottom: 5px;
          font-weight: bold;
        }
        
        input {
          width: 100%;
          padding: 8px;
          border: 1px solid #ccc;
          border-radius: 4px;
        }
        
        .outcome-input {
          display: flex;
          margin-bottom: 8px;
        }
        
        .outcome-input input {
          flex-grow: 1;
        }
        
        .remove-button {
          background-color: #f44336;
          color: white;
          border: none;
          border-radius: 50%;
          width: 24px;
          height: 24px;
          margin-left: 8px;
          cursor: pointer;
        }
        
        .add-outcome-button {
          background-color: transparent;
          color: #2196f3;
          border: none;
          padding: 4px 0;
          cursor: pointer;
          text-align: left;
        }
        
        .submit-button {
          background-color: #9146FF;
          color: white;
          border: none;
          padding: 10px 15px;
          border-radius: 4px;
          font-weight: bold;
          cursor: pointer;
          width: 100%;
        }
        
        .submit-button:hover {
          background-color: #7d2ff3;
        }
        
        .submit-button:disabled {
          background-color: #cccccc;
          cursor: not-allowed;
        }
      `}</style>
        </div>
    );
}