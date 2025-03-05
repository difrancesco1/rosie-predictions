// components/integrations/LeagueIntegration.js
import { useState, useEffect } from 'react';
import {
    connectLeagueAccount,
    getAllLeagueAccounts,
    getActiveLeagueAccount,
    setAccountActive,
    updateLeagueSettings,
    disconnectLeagueAccount
} from '../../services/leagueApi';
import styles from './LeagueIntegration.module.css';

export default function LeagueIntegration() {
    const [summonerName, setSummonerName] = useState('');
    const [accounts, setAccounts] = useState([]);
    const [activeAccount, setActiveAccount] = useState(null);
    const [isLoading, setIsLoading] = useState(false);
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

            // Get active account
            const active = await getActiveLeagueAccount();
            setActiveAccount(active);
        } catch (err) {
            console.error('Error fetching accounts:', err);
            setError('Failed to load accounts. Please try again.');
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

    const handleSetActive = async (accountId) => {
        try {
            setIsLoading(true);
            setError(null);

            // Set account as active
            const updatedAccount = await setAccountActive(accountId);

            // Reload accounts to reflect changes
            await loadAccountsData();
        } catch (err) {
            setError('Failed to set active account: ' + (err.message || 'Unknown error'));
        } finally {
            setIsLoading(false);
        }
    };

    const handleUpdateSettings = async (accountId, settingName, value) => {
        try {
            setIsLoading(true);
            setError(null);

            // Prepare settings object
            const settings = {};
            settings[settingName] = value;

            // Update settings
            await updateLeagueSettings(accountId, settings);

            // Reload accounts to reflect changes
            await loadAccountsData();
        } catch (err) {
            setError('Failed to update settings: ' + (err.message || 'Unknown error'));
        } finally {
            setIsLoading(false);
        }
    };

    const handleDisconnect = async (accountId) => {
        if (!window.confirm('Are you sure you want to disconnect this account?')) {
            return;
        }

        try {
            setIsLoading(true);
            setError(null);

            // Disconnect account
            await disconnectLeagueAccount(accountId);

            // Reload accounts
            await loadAccountsData();
        } catch (err) {
            setError('Failed to disconnect: ' + (err.message || 'Unknown error'));
        } finally {
            setIsLoading(false);
        }
    };

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
            {accounts.length > 0 && (
                <div className={styles.accountsList}>
                    <h4 className={styles.sectionTitle}>Connected Accounts</h4>

                    {accounts.map(account => (
                        <div
                            key={account.id}
                            className={`${styles.accountItem} ${account.active ? styles.activeAccount : ''}`}
                        >
                            <div className={styles.accountInfo}>
                                <div className={styles.summonerName}>{account.summonerName}</div>
                                {account.active && <div className={styles.activeLabel}>Active</div>}
                            </div>

                            <div className={styles.accountActions}>
                                {!account.active && (
                                    <button
                                        className={styles.actionButton}
                                        onClick={() => handleSetActive(account.id)}
                                        disabled={isLoading}
                                    >
                                        Set Active
                                    </button>
                                )}

                                <button
                                    className={`${styles.actionButton} ${styles.disconnectButton}`}
                                    onClick={() => handleDisconnect(account.id)}
                                    disabled={isLoading}
                                >
                                    Disconnect
                                </button>
                            </div>

                            {account.active && (
                                <div className={styles.accountSettings}>
                                    <div className={styles.settingRow}>
                                        <span>Auto-create predictions</span>
                                        <div className={styles.toggle}>
                                            <input
                                                type="checkbox"
                                                id={`create-${account.id}`}
                                                checked={account.autoCreatePredictions}
                                                onChange={(e) => handleUpdateSettings(
                                                    account.id,
                                                    'autoCreatePredictions',
                                                    e.target.checked
                                                )}
                                                disabled={isLoading}
                                            />
                                            <span className={styles.slider}></span>
                                        </div>
                                    </div>

                                    <div className={styles.settingRow}>
                                        <span>Auto-resolve predictions</span>
                                        <div className={styles.toggle}>
                                            <input
                                                type="checkbox"
                                                id={`resolve-${account.id}`}
                                                checked={account.autoResolvePredictions}
                                                onChange={(e) => handleUpdateSettings(
                                                    account.id,
                                                    'autoResolvePredictions',
                                                    e.target.checked
                                                )}
                                                disabled={isLoading}
                                            />
                                            <span className={styles.slider}></span>
                                        </div>
                                    </div>
                                </div>
                            )}
                        </div>
                    ))}
                </div>
            )}

            {/* Connect form */}
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