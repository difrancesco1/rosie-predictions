// components/predictions/CreatePredictionForm.js
import { useState, useEffect } from 'react';
import { createPrediction } from '../../services/predictions.js';
import styles from './CreatePredictionForm.module.css';

// Template storage key
const TEMPLATE_STORAGE_KEY = 'prediction_templates';

export default function CreatePredictionForm({ onSuccess }) {
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState(null);
    const [title, setTitle] = useState('');
    const [outcomes, setOutcomes] = useState(['', '']);
    const [predictionWindow, setPredictionWindow] = useState(60);
    const [sessionId, setSessionId] = useState('');
    const [formStep, setFormStep] = useState(1); // 1: Title, 2: Outcomes, 3: Duration
    const [templates, setTemplates] = useState([]);
    const [showTemplates, setShowTemplates] = useState(false);
    const [saveAsTemplate, setSaveAsTemplate] = useState(false);

    // Load templates on component mount
    useEffect(() => {
        const loadedTemplates = localStorage.getItem(TEMPLATE_STORAGE_KEY);
        if (loadedTemplates) {
            try {
                setTemplates(JSON.parse(loadedTemplates));
            } catch (e) {
                console.error('Failed to parse templates:', e);
            }
        }
    }, []);

    const handleOutcomeChange = (index, value) => {
        const newOutcomes = [...outcomes];
        newOutcomes[index] = value;
        setOutcomes(newOutcomes);
    };

    const addOutcome = () => {
        if (outcomes.length < 5) {
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
            setError('Please enter a title');
            setFormStep(1);
            return;
        }

        const filteredOutcomes = outcomes.filter(o => o.trim() !== '');
        if (filteredOutcomes.length < 2) {
            setError('Need at least two outcomes');
            setFormStep(2);
            return;
        }

        setIsLoading(true);
        setError(null);

        try {
            const predictionData = {
                title,
                outcomes: filteredOutcomes,
                predictionWindow,
                sessionId: sessionId || null,
            };

            await createPrediction(predictionData);

            // Save as template if checkbox is checked
            if (saveAsTemplate) {
                saveTemplate(predictionData);
            }

            // Reset form
            setTitle('');
            setOutcomes(['', '']);
            setPredictionWindow(60);
            setFormStep(1);
            setSaveAsTemplate(false);

            if (onSuccess) {
                onSuccess();
            }
        } catch (error) {
            setError(error.message || 'Failed to create prediction');
        } finally {
            setIsLoading(false);
        }
    };

    const saveTemplate = (predictionData) => {
        // Create a new template
        const newTemplate = {
            id: Date.now().toString(),
            ...predictionData,
        };

        // Add to templates array
        const updatedTemplates = [...templates, newTemplate];

        // Save to localStorage
        localStorage.setItem(TEMPLATE_STORAGE_KEY, JSON.stringify(updatedTemplates));

        // Update state
        setTemplates(updatedTemplates);
    };

    const loadTemplate = (template) => {
        setTitle(template.title);
        setOutcomes(template.outcomes.length > 0 ? template.outcomes : ['', '']);
        setPredictionWindow(template.predictionWindow || 60);
        setSessionId(template.sessionId || '');
        setShowTemplates(false);
        setFormStep(1); // Reset to first step to let user review
    };

    const deleteTemplate = (id, e) => {
        e.stopPropagation(); // Prevent triggering the parent onClick
        const updatedTemplates = templates.filter(template => template.id !== id);
        localStorage.setItem(TEMPLATE_STORAGE_KEY, JSON.stringify(updatedTemplates));
        setTemplates(updatedTemplates);
    };

    const nextStep = () => {
        if (formStep === 1 && !title.trim()) {
            setError('Please enter a title');
            return;
        }

        if (formStep === 2) {
            const filteredOutcomes = outcomes.filter(o => o.trim() !== '');
            if (filteredOutcomes.length < 2) {
                setError('Need at least two outcomes');
                return;
            }
        }

        setError(null);
        setFormStep(formStep + 1);
    };

    const prevStep = () => {
        setError(null);
        setFormStep(formStep - 1);
    };

    return (
        <div className={styles.createPredictionForm}>
            <div className={styles.formHeader}>
                <h3 className={styles.formTitle}>New Prediction</h3>
                <button
                    type="button"
                    className={styles.templateButton}
                    onClick={() => setShowTemplates(!showTemplates)}
                >
                    {showTemplates ? 'Hide Templates' : 'Templates'}
                </button>
            </div>

            {error && <div className={styles.errorMessage}>{error}</div>}

            {/* Template selector */}
            {showTemplates && (
                <div className={styles.templateSelector}>
                    <h4 className={styles.templateTitle}>Saved Templates</h4>
                    {templates.length === 0 ? (
                        <p className={styles.emptyTemplates}>No saved templates yet</p>
                    ) : (
                        <div className={styles.templateList}>
                            {templates.map(template => (
                                <div
                                    key={template.id}
                                    className={styles.templateItem}
                                    onClick={() => loadTemplate(template)}
                                >
                                    <span className={styles.templateName}>{template.title}</span>
                                    <button
                                        type="button"
                                        className={styles.templateDelete}
                                        onClick={(e) => deleteTemplate(template.id, e)}
                                        aria-label="Delete template"
                                    >
                                        ×
                                    </button>
                                </div>
                            ))}
                        </div>
                    )}
                </div>
            )}

            <form onSubmit={handleSubmit}>
                {formStep === 1 && (
                    <div className={styles.formStep}>
                        <div className={styles.formGroup}>
                            <label htmlFor="prediction-title"></label>
                            <input
                                id="prediction-title"
                                type="text"
                                value={title}
                                onChange={(e) => setTitle(e.target.value)}
                                placeholder="Prediction Title"
                                disabled={isLoading}
                                required
                            />
                        </div>
                        <button
                            type="button"
                            onClick={nextStep}
                            disabled={isLoading}
                            className={styles.nextButton}
                        >
                            Next
                        </button>
                    </div>
                )}

                {formStep === 2 && (
                    <div className={styles.formStep}>
                        <div className={styles.formGroup}>
                            <label>Outcomes (2-5)</label>
                            {outcomes.map((outcome, index) => (
                                <div key={index} className={styles.outcomeInput}>
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
                                            className={styles.removeButton}
                                            aria-label="Remove outcome"
                                        >
                                            ×
                                        </button>
                                    )}
                                </div>
                            ))}

                            {outcomes.length < 5 && (
                                <button
                                    type="button"
                                    onClick={addOutcome}
                                    disabled={isLoading}
                                    className={styles.addOutcomeButton}
                                >
                                    + Add outcome
                                </button>
                            )}
                        </div>
                        <div className={styles.buttonGroup}>
                            <button
                                type="button"
                                onClick={prevStep}
                                disabled={isLoading}
                                className={styles.backButton}
                            >
                                Back
                            </button>
                            <button
                                type="button"
                                onClick={nextStep}
                                disabled={isLoading}
                                className={styles.nextButton}
                            >
                                Next
                            </button>
                        </div>
                    </div>
                )}

                {formStep === 3 && (
                    <div className={styles.formStep}>
                        <div className={styles.formGroup}>
                            <label>Duration</label>
                            <div className={styles.durationButtons}>
                                <button
                                    type="button"
                                    onClick={() => setPredictionWindow(60)}
                                    className={`${styles.durationButton} ${predictionWindow === 60 ? styles.durationButtonActive : ''}`}
                                >
                                    1 minute
                                </button>
                                <button
                                    type="button"
                                    onClick={() => setPredictionWindow(120)}
                                    className={`${styles.durationButton} ${predictionWindow === 120 ? styles.durationButtonActive : ''}`}
                                >
                                    2 minutes
                                </button>
                                <button
                                    type="button"
                                    onClick={() => setPredictionWindow(300)}
                                    className={`${styles.durationButton} ${predictionWindow === 300 ? styles.durationButtonActive : ''}`}
                                >
                                    5 minutes
                                </button>
                            </div>
                        </div>

                        <div className={styles.saveTemplateOption}>
                            <input
                                type="checkbox"
                                id="save-template"
                                checked={saveAsTemplate}
                                onChange={() => setSaveAsTemplate(!saveAsTemplate)}
                            />
                            <label htmlFor="save-template">Save as template</label>
                        </div>

                        <div className={styles.buttonGroup}>
                            <button
                                type="button"
                                onClick={prevStep}
                                disabled={isLoading}
                                className={styles.backButton}
                            >
                                Back
                            </button>
                            <button
                                type="submit"
                                disabled={isLoading}
                                className={styles.submitButton}
                            >
                                {isLoading ? 'Creating...' : 'Create'}
                            </button>
                        </div>
                    </div>
                )}
            </form>
        </div>
    );
}