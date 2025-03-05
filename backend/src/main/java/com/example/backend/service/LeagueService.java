package com.example.backend.service;

import com.example.backend.model.LeagueAccount;
import com.example.backend.repository.LeagueAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class LeagueService {
    private static final Logger logger = LoggerFactory.getLogger(LeagueService.class);

    private final LeagueAccountRepository leagueAccountRepository;
    private final TwitchPredictionService predictionService;

    @Autowired
    public LeagueService(LeagueAccountRepository leagueAccountRepository,
            TwitchPredictionService predictionService) {
        this.leagueAccountRepository = leagueAccountRepository;
        this.predictionService = predictionService;
    }

    /**
     * Connect a Twitch user's account to their League of Legends summoner name
     * This allows multiple accounts per user
     */
    public LeagueAccount connectAccount(String userId, String summonerName) throws IOException {
        logger.info("Connecting account for userId: {}, summonerName: {}", userId, summonerName);

        // Set all existing accounts to inactive first
        List<LeagueAccount> existingAccounts = leagueAccountRepository.findAllByUserId(userId);
        for (LeagueAccount existingAccount : existingAccounts) {
            existingAccount.setActive(false);
            leagueAccountRepository.save(existingAccount);
        }

        // Create new account
        LeagueAccount account = new LeagueAccount(userId, summonerName);
        account.setSummonerId("test-summoner-id-" + System.currentTimeMillis());
        account.setPuuid("test-puuid-" + System.currentTimeMillis());
        account.setRegion("na1");
        account.setActive(true); // Set the new account as active

        return leagueAccountRepository.save(account);
    }

    /**
     * Deactivate all accounts for a user
     */
    private void deactivateAllAccounts(String userId) {
        List<LeagueAccount> accounts = leagueAccountRepository.findAllByUserId(userId);

        // Guard against null accounts in the list
        if (accounts != null) {
            for (LeagueAccount account : accounts) {
                if (account != null) { // Add null check before using the account
                    account.setActive(false);
                    leagueAccountRepository.save(account);
                }
            }
        }
    }

    /**
     * Get all accounts for a user
     */
    public List<LeagueAccount> getAllAccountsByUserId(String userId) {
        return leagueAccountRepository.findAllByUserId(userId);
    }

    /**
     * Get active account for a user
     */
    public Optional<LeagueAccount> getActiveAccountByUserId(String userId) {
        return leagueAccountRepository.findByUserIdAndIsActiveTrue(userId);
    }

    /**
     * Get specific account by ID
     */
    public Optional<LeagueAccount> getAccountById(UUID accountId) {
        return leagueAccountRepository.findById(accountId);
    }

    /**
     * Set an account as active
     */
    public LeagueAccount setAccountActive(String userId, UUID accountId) {
        // First, deactivate all accounts for this user
        deactivateAllAccounts(userId);

        // Then, activate the specified account
        LeagueAccount account = leagueAccountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        // Verify the account belongs to the user
        if (!account.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Account does not belong to this user");
        }

        account.setActive(true);
        return leagueAccountRepository.save(account);
    }

    /**
     * Update account settings
     */
    public LeagueAccount updateAccountSettings(String userId, UUID accountId,
            boolean autoCreate, boolean autoResolve) {
        LeagueAccount account = leagueAccountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        // Verify the account belongs to the user
        if (!account.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Account does not belong to this user");
        }

        account.setAutoCreatePredictions(autoCreate);
        account.setAutoResolvePredictions(autoResolve);
        return leagueAccountRepository.save(account);
    }

    /**
     * Delete a specific League account
     */
    public void disconnectAccount(String userId, UUID accountId) {
        LeagueAccount account = leagueAccountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        // Verify the account belongs to the user
        if (!account.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Account does not belong to this user");
        }

        leagueAccountRepository.deleteById(accountId);
    }

    /**
     * Check for new games and create/resolve predictions
     */
    @Scheduled(fixedDelay = 60000)
    public void checkForGames() {
        logger.info("Checking for League games...");

        // Find active accounts with auto features enabled
        List<LeagueAccount> autoCreateAccounts = leagueAccountRepository
                .findByAutoCreatePredictionsTrueAndIsActiveTrue();
        List<LeagueAccount> autoResolveAccounts = leagueAccountRepository
                .findByAutoResolvePredictionsTrueAndIsActiveTrue();

        // Check for active games and create predictions
        for (LeagueAccount account : autoCreateAccounts) {
            try {
                logger.info("Checking games for account: {}", account.getSummonerName());
                // Implementation will go here later
            } catch (Exception e) {
                logger.error("Error checking games for account {}: {}", account.getSummonerName(), e.getMessage());
            }
        }

        // Check for completed games and resolve predictions
        for (LeagueAccount account : autoResolveAccounts) {
            try {
                logger.info("Checking for completed games for account: {}", account.getSummonerName());
                // Implementation will go here later
            } catch (Exception e) {
                logger.error("Error resolving predictions for account {}: {}", account.getSummonerName(),
                        e.getMessage());
            }
        }
    }
}