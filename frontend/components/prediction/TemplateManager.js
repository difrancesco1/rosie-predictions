// components/prediction/TemplateManager.js
import { useState, useEffect } from 'react';
import { getUserData } from '../../utils/auth';
import styles from './TemplateManager.module.css';

export default function TemplateManager() {
    const [templates, setTemplates] = useState([]);
    const [summoners, setSummoners] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState(null);
    const [showForm, setShowForm] = useState(false);
    const [formData, setFormData] = useState({
        title: "Will {summonerName} win their game?",
        outcome1: "Win",
        outcome2: "Loss",
        duration: 1800
    });

    // Get user data
    const userData = getUserData();
    const userId = userData?.userId;

    // Load templates and summoners on component mount
    useEffect(() => {
        if (userId) {
            fetchTemplates();
            fetchSummoners();
        }
    }, [userId]);

    // Fetch templates from API
    const fetchTemplates = async () => {
        try {
            setIsLoading(true);
            const response = await fetch(`/api/templates/${userId}`);

            if (!response.ok) {
                throw new Error('Failed to fetch templates');
            }

            const data = await response.json();
            setTemplates(data);
        } catch (err) {
            setError(`Error loading templates: ${err.message}`);
        } finally {
            setIsLoading(false);
        }
    };

    // Fetch summoners from API
    const fetchSummoners = async () => {
        try {
            const response = await fetch(`/api/league/${userId}/accounts`);

            if (!response.ok) {
                throw new Error('Failed to fetch summoners');
            }

            const data = await response.json();
            setSummoners(data);
        } catch (err) {
            setError(`Error loading summoners: ${err.message}`);
        }
    };

    // Handle form input changes
    const handleInputChange = (e) => {
        const { name, value } = e.target;
        setFormData({
            ...formData,
            [name]: name === 'duration' ? parseInt(value) : value
        });
    };

    // Create new template
    const handleCreateTemplate = async (e) => {
        e.preventDefault();

        try {
            setIsLoading(true);

            const response = await fetch(`/api/templates/${userId}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(formData)
            });

            if (!response.ok) {
                throw new Error('Failed to create template');
            }

            // Refresh templates list
            await fetchTemplates();
            setShowForm(false);
        } catch (err) {
            setError(`Error creating template: ${err.message}`);
        } finally {
            setIsLoading(false);
        }
    };

    // Assign template to summoner
    const handleAssignTemplate = async (templateId, accountId) => {
        try {
            setIsLoading(true);

            const response = await fetch(`/api/templates/${userId}/assign/${accountId}/${templateId}`, {
                method: 'POST'
            });

            if (!response.ok) {
                throw new Error('Failed to assign template');
            }

            // Refresh summoners list to show assignment
            await fetchSummoners();
        } catch (err) {
            setError(`Error assigning template: ${err.message}`);
        } finally {
            setIsLoading(false);
        }
    };

    // Remove template from summoner
    const handleRemoveTemplate = async (accountId) => {
        try {
            setIsLoading(true);

            const response = await fetch(`/api/templates/${userId}/assign/${accountId}`, {
                method: 'DELETE'
            });

            if (!response.ok) {
                throw new Error('Failed to remove template');
            }

            // Refresh summoners list
            await fetchSummoners();
        } catch (err) {
            setError(`Error removing template: ${err.message}`);
        } finally {
            setIsLoading(false);
        }
    };

    // Loading state
    if (isLoading && templates.length === 0) {
        return <div className={styles.loading}>Loading templates...</div>;
    }

    return (
        <div className={styles.templateManager}>
            <h2>Prediction Templates</h2>

            {error && <div className={styles.error}>{error}</div>}

            {/* Templates list */}
            <div className={styles.templateList}>
                <h3>Your Templates</h3>

                {templates.length === 0 ? (
                    <p>No templates yet. Create your first template.</p>
                ) : (
                    <div className={styles.templates}>
                        {templates.map(template => (
                            <div key={template.id} className={styles.templateCard}>
                                <h4>{template.title}</h4>
                                <div className={styles.outcomes}>
                                    <span>{template.outcome1} / {template.outcome2}</span>
                                </div>
                                <div className={styles.duration}>
                                    Duration: {Math.floor(template.duration / 60)} minutes
                                </div>

                                {/* Assign template dropdown */}
                                <div className={styles.assignSection}>
                                    <select
                                        onChange={(e) => {
                                            if (e.target.value) {
                                                handleAssignTemplate(template.id, e.target.value);
                                            }
                                        }}
                                        value=""
                                    >
                                        <option value="">Assign to summoner...</option>
                                        {summoners.map(summoner => (
                                            <option key={summoner.id} value={summoner.id}>
                                                {summoner.summonerName}
                                            </option>
                                        ))}
                                    </select>
                                </div>
                            </div>
                        ))}
                    </div>
                )}

                {/* New template form toggle */}
                <button
                    className={styles.newTemplateBtn}
                    onClick={() => setShowForm(!showForm)}
                >
                    {showForm ? 'Cancel' : '+ New Template'}
                </button>

                {/* New template form */}
                {showForm && (
                    <form onSubmit={handleCreateTemplate} className={styles.templateForm}>
                        <div className={styles.formGroup}>
                            <label htmlFor="title">Title:</label>
                            <input
                                type="text"
                                id="title"
                                name="title"
                                value={formData.title}
                                onChange={handleInputChange}
                                required
                            />
                            <small>Use {"{summonerName}"} to insert the summoner name</small>
                        </div>

                        <div className={styles.formGroup}>
                            <label htmlFor="outcome1">Outcome 1:</label>
                            <input
                                type="text"
                                id="outcome1"
                                name="outcome1"
                                value={formData.outcome1}
                                onChange={handleInputChange}
                                required
                            />
                        </div>

                        <div className={styles.formGroup}>
                            <label htmlFor="outcome2">Outcome 2:</label>
                            <input
                                type="text"
                                id="outcome2"
                                name="outcome2"
                                value={formData.outcome2}
                                onChange={handleInputChange}
                                required
                            />
                        </div>

                        <div className={styles.formGroup}>
                            <label htmlFor="duration">Duration (minutes):</label>
                            <input
                                type="number"
                                id="duration"
                                name="duration"
                                value={formData.duration / 60}
                                onChange={(e) => setFormData({ ...formData, duration: parseInt(e.target.value) * 60 })}
                                min="1"
                                max="60"
                                required
                            />
                        </div>

                        <button type="submit" className={styles.submitBtn} disabled={isLoading}>
                            {isLoading ? 'Creating...' : 'Create Template'}
                        </button>
                    </form>
                )}
            </div>

            {/* Summoners with assigned templates */}
            <div className={styles.summonerSection}>
                <h3>Summoner Templates</h3>

                {summoners.length === 0 ? (
                    <p>No summoners found. Add a League account first.</p>
                ) : (
                    <div className={styles.summoners}>
                        {summoners.map(summoner => (
                            <div key={summoner.id} className={styles.summonerCard}>
                                <div className={styles.summonerInfo}>
                                    <h4>{summoner.summonerName}</h4>
                                    <div className={styles.activeStatus}>
                                        {summoner.active ? '(Active)' : ''}
                                    </div>
                                </div>

                                <div className={styles.templateStatus}>
                                    {summoner.activeTemplateId ? (
                                        <div className={styles.activeTemplate}>
                                            <p>Using template: {
                                                templates.find(t => t.id === summoner.activeTemplateId)?.title || 'Unknown template'
                                            }</p>
                                            <button
                                                onClick={() => handleRemoveTemplate(summoner.id)}
                                                className={styles.removeBtn}
                                            >
                                                Remove
                                            </button>
                                        </div>
                                    ) : (
                                        <p>No template assigned</p>
                                    )}
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </div>
        </div>
    );
}