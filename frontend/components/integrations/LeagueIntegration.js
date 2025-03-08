// Modified version of LeagueIntegration.js
import { useState, useEffect } from 'react';
import {
    connectLeagueAccount,
    getAllLeagueAccounts,
    updateLeagueSettings,
    disconnectLeagueAccount
} from '../../services/leagueApi';
import styles from './LeagueIntegration.module.css';

export default function LeagueIntegration() {
    const [summonerName, setSummonerName] = useState('');
    const [accounts, setAccounts] = useState([]);
    const [isLoading, setIsLoading] = useState(true); // Start with loading state
    const [error, setError] = useState(null);
    const [showConnectForm, setShowConnectForm] = useState(false);

    // Load accounts data on component mount
    useEffect(() => {
        loadAccountsData();
    }, []);

    const loadAccountsData = async () => {
        try {
            setIsLoading(true);
            setError(null);

            // Get all accounts
            const allAccounts = await getAllLeagueAccounts();
            setAccounts(allAccounts || []);
        } catch (err) {
            console.error('Error fetching accounts:', err);
            setError('Failed to load accounts. Please try again.');
            // Ensure accounts is an array even on error
            setAccounts([]);
        } finally {
            setIsLoading(false);
        }
    };

    const handleConnect = async (e) => {
        e.preventDefault();

        if (!summonerName || !summonerName.includes('#')) {
            setError('Please enter a valid summoner name including the # tag');
            return;
        }

        // Clear previous errors
        setError(null);
        setIsLoading(true);

        try {
            // Connect account through the backend
            await connectLeagueAccount(summonerName);

            // Reset form
            setSummonerName('');
            setShowConnectForm(false);

            // Reload accounts
            await loadAccountsData();
        } catch (err) {
            setError('Failed to connect: ' + (err.message || 'Unknown error'));
        } finally {
            setIsLoading(false);
        }
    };

    const handleUpdateSettings = async (account, settingName, value) => {
        try {
            setIsLoading(true);
            setError(null);

            console.log(`Updating ${settingName} to ${value} for account ${account.id}`);

            // Prepare settings object
            const settings = {};
            settings[settingName] = value;

            // Update settings
            await updateLeagueSettings(account.id, settings);

            // Reload accounts to reflect changes
            await loadAccountsData();
        } catch (err) {
            console.error('Error updating settings:', err);
            setError('Failed to update settings: ' + (err.message || 'Unknown error'));
        } finally {
            setIsLoading(false);
        }
    };

    const handleDisconnect = async (account) => {
        if (!account || !account.id) {
            console.error('Invalid account data:', account);
            return;
        }

        if (!window.confirm('Are you sure you want to disconnect this account?')) {
            return;
        }

        try {
            setIsLoading(true);
            setError(null);

            // Disconnect account
            await disconnectLeagueAccount(account.id);

            // Reload accounts
            await loadAccountsData();
        } catch (err) {
            setError('Failed to disconnect: ' + (err.message || 'Unknown error'));
        } finally {
            setIsLoading(false);
        }
    };

    // Show loading state
    if (isLoading && accounts.length === 0) {
        return (
            <div className={styles.leagueIntegration}>
                <h3 className={styles.title}>League of Legends Integration</h3>
                <div className={styles.loadingState}>Loading account data...</div>
            </div>
        );
    }

    return (
        <div className={styles.leagueIntegration}>
            <h3 className={styles.title}>League of Legends Integration</h3>

            {error && (
                <div className={styles.error}>{error}</div>
            )}

            {/* Accounts list section */}
            {accounts && accounts.length > 0 && (
                <div className={styles.accountsList}>
                    <h4 className={styles.sectionTitle}>Connected Accounts</h4>

                    <div className={styles.accountsContainer}>
                        {accounts.map(account => {
                            // Skip rendering if account is null or missing required properties
                            if (!account || !account.id) return null;

                            return (
                                <div
                                    key={account.id}
                                    className={styles.accountItem}
                                >
                                    <div className={styles.accountInfo}>
                                        <div className={styles.summonerName}>
                                            {account.summonerName || 'Unknown Summoner'}
                                        </div>
                                    </div>

                                    <div className={styles.accountSettings}>
                                        <div className={styles.settingRow}>
                                            <span>Auto-create predictions</span>
                                            <label>
                                                <input
                                                    type="checkbox"
                                                    checked={account.autoCreatePredictions || false}
                                                    onChange={(e) => {
                                                        console.log("Toggle clicked!", e.target.checked);
                                                        handleUpdateSettings(
                                                            account,
                                                            'autoCreatePredictions',
                                                            e.target.checked
                                                        );
                                                    }}
                                                    style={{ marginLeft: '10px' }}
                                                />
                                                {account.autoCreatePredictions ? "On" : "Off"}
                                            </label>
                                        </div>

                                        <div className={styles.settingRow}>
                                            <span>Auto-resolve predictions</span>
                                            <label>
                                                <input
                                                    type="checkbox"
                                                    checked={account.autoResolvePredictions || false}
                                                    onChange={(e) => {
                                                        console.log("Resolve toggle clicked!", e.target.checked);
                                                        handleUpdateSettings(
                                                            account,
                                                            'autoResolvePredictions',
                                                            e.target.checked
                                                        );
                                                    }}
                                                    style={{ marginLeft: '10px' }}
                                                />
                                                {account.autoResolvePredictions ? "On" : "Off"}
                                            </label>
                                        </div>
                                    </div>

                                    <div className={styles.accountActions}>
                                        <button
                                            className={`${styles.actionButton} ${styles.disconnectButton}`}
                                            onClick={() => handleDisconnect(account)}
                                            disabled={isLoading}
                                        >
                                            Disconnect
                                        </button>
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                </div>
            )}

            {/* Connect form or Add button */}
            {showConnectForm ? (
                <form onSubmit={handleConnect} className={styles.connectForm}>
                    <div className={styles.inputGroup}>
                        <label htmlFor="summoner-name">Summoner Name</label>
                        <input
                            id="summoner-name"
                            type="text"
                            value={summonerName}
                            onChange={(e) => setSummonerName(e.target.value)}
                            placeholder="Name#TAG"
                            disabled={isLoading}
                            required
                        />
                        <p className={styles.hint}>Enter your League of Legends Summoner Name including the # tag</p>
                    </div>

                    <div className={styles.formButtons}>
                        <button
                            type="button"
                            onClick={() => setShowConnectForm(false)}
                            disabled={isLoading}
                            className={styles.cancelButton}
                        >
                            Cancel
                        </button>
                        <button
                            type="submit"
                            disabled={isLoading}
                            className={styles.connectButton}
                        >
                            {isLoading ? 'Connecting...' : 'Connect Account'}
                        </button>
                    </div>
                </form>
            ) : (
                <div className={styles.addAccountSection}>
                    <button
                        onClick={() => setShowConnectForm(true)}
                        className={styles.addAccountButton}
                        disabled={isLoading}
                    >
                        + Add League Account
                    </button>
                </div>
            )}
        </div>
    );
}