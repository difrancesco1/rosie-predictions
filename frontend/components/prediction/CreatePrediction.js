// components/prediction/CreatePrediction.js
import { useState } from 'react';
import { createPrediction } from '../../services/api';  // Your API call to the backend

const CreatePrediction = () => {
    const [title, setTitle] = useState('');
    const [description, setDescription] = useState('');
    const [optionA, setOptionA] = useState('');
    const [optionB, setOptionB] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    const handleCreatePrediction = async (e) => {
        e.preventDefault();
        setLoading(true);
        setError('');

        try {
            await createPrediction({ title, description, optionA, optionB });
            setTitle('');
            setDescription('');
            setOptionA('');
            setOptionB('');
            alert('Prediction created successfully!');
        } catch (error) {
            setError('Failed to create prediction');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div>
            <h2>Create Prediction</h2>
            <form onSubmit={handleCreatePrediction}>
                <input
                    type="text"
                    placeholder="Title"
                    value={title}
                    onChange={(e) => setTitle(e.target.value)}
                    required
                />
                <textarea
                    placeholder="Description"
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                    required
                />
                <input
                    type="text"
                    placeholder="Option A"
                    value={optionA}
                    onChange={(e) => setOptionA(e.target.value)}
                    required
                />
                <input
                    type="text"
                    placeholder="Option B"
                    value={optionB}
                    onChange={(e) => setOptionB(e.target.value)}
                    required
                />
                <button type="submit" disabled={loading}>
                    {loading ? 'Creating...' : 'Create Prediction'}
                </button>
                {error && <div>{error}</div>}
            </form>
        </div>
    );
};

export default CreatePrediction;
