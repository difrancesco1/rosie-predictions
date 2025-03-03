// components/prediction/ManagePrediction.js
import { useState } from 'react';
import { resolvePrediction, cancelPrediction } from '../../services/api';  // API calls to resolve or cancel predictions

const ManagePrediction = ({ predictionId }) => {
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    const handleResolve = async () => {
        setLoading(true);
        setError('');
        try {
            await resolvePrediction(predictionId);
            alert('Prediction resolved!');
        } catch (error) {
            setError('Failed to resolve prediction');
        } finally {
            setLoading(false);
        }
    };

    const handleCancel = async () => {
        setLoading(true);
        setError('');
        try {
            await cancelPrediction(predictionId);
            alert('Prediction canceled!');
        } catch (error) {
            setError('Failed to cancel prediction');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div>
            <button onClick={handleResolve} disabled={loading}>
                {loading ? 'Resolving...' : 'Resolve Prediction'}
            </button>
            <button onClick={handleCancel} disabled={loading}>
                {loading ? 'Canceling...' : 'Cancel Prediction'}
            </button>
            {error && <div>{error}</div>}
        </div>
    );
};

export default ManagePrediction;
