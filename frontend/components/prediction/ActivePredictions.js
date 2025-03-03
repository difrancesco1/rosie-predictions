// components/prediction/ActivePredictions.js
import { useEffect, useState } from 'react';
import { getActivePredictions } from '../../services/api';  // Your API call to fetch active predictions

const ActivePredictions = () => {
    const [predictions, setPredictions] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        const fetchPredictions = async () => {
            try {
                const data = await getActivePredictions();
                setPredictions(data);
            } catch (error) {
                setError('Failed to fetch active predictions');
            } finally {
                setLoading(false);
            }
        };

        fetchPredictions();
    }, []);

    if (loading) return <div>Loading predictions...</div>;
    if (error) return <div>{error}</div>;

    return (
        <div>
            <h2>Active Predictions</h2>
            {predictions.length === 0 ? (
                <p>No active predictions</p>
            ) : (
                <ul>
                    {predictions.map((prediction) => (
                        <li key={prediction.id}>
                            <h3>{prediction.title}</h3>
                            <p>{prediction.description}</p>
                            <div>Option A: {prediction.optionA}</div>
                            <div>Option B: {prediction.optionB}</div>
                        </li>
                    ))}
                </ul>
            )}
        </div>
    );
};

export default ActivePredictions;
