package com.example.backend.service;

import com.example.backend.config.LeagueConfig;
import com.example.backend.model.LeagueAccount;
import com.example.backend.model.Prediction;
import com.example.backend.model.PredictionTemplate;
import com.example.backend.repository.LeagueAccountRepository;
import com.example.backend.repository.PredictionTemplateRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class LeagueService {
    private static final Logger logger = LoggerFactory.getLogger(LeagueService.class);

    private final LeagueAccountRepository leagueAccountRepository;
    private final TwitchPredictionService predictionService;
    private final LeagueConfig leagueConfig;
    private final RestTemplate restTemplate;

    // Simple class to track game and prediction info
    private static class GameTracker {
        String summonerId;
        String userId;
        String gameId;
        String predictionId;
        boolean inGame;

        public GameTracker(String summonerId, String userId, String gameId, String predictionId) {
            this.summonerId = summonerId;
            this.userId = userId;
            this.gameId = gameId;
            this.predictionId = predictionId;
            this.inGame = true;
        }
    }

    // Map to track active games by summoner ID
    private final Map<String, GameTracker> activeGames = new HashMap<>();
    @Autowired
    private PredictionTemplateRepository templateRepository;

    @Autowired
    public LeagueService(LeagueAccountRepository leagueAccountRepository,
            TwitchPredictionService predictionService,
            LeagueConfig leagueConfig,
            RestTemplate restTemplate) {
        this.leagueAccountRepository = leagueAccountRepository;
        this.predictionService = predictionService;
        this.leagueConfig = leagueConfig;
        this.restTemplate = restTemplate;
    }

    /**
     * Connect a Twitch user's account to their League of Legends summoner name
     */
    public LeagueAccount connectAccount(String userId, String summonerName) {
        logger.info("Connecting account for userId: {}, summonerName: {}", userId, summonerName);

        // Deactivate all existing accounts for this user
        deactivateAllAccounts(userId);

        // Create new account
        LeagueAccount account = new LeagueAccount(userId, summonerName);

        // In a real implementation, we would call the Riot API to get these IDs
        // For mock mode, we'll use generated values
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

        if (accounts != null) {
            for (LeagueAccount account : accounts) {
                if (account != null) {
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

    @Scheduled(fixedDelayString = "${league.polling-interval:60}000")
    public void checkGameStatus() {
        logger.info("Checking summoner game status...");

        List<LeagueAccount> accounts = leagueAccountRepository.findAll();

        logger.info("Found {} accounts to check", accounts.size());

        for (LeagueAccount account : accounts) {
            if (!account.isAutoCreatePredictions() && !account.isAutoResolvePredictions()) {
                continue;
            }

            try {
                processAccount(account);
            } catch (Exception e) {
                logger.error("Error processing account {}: {}", account.getSummonerName(), e.getMessage(), e);
            }
        }
    }

    /**
     * Process a single account for game status and prediction management
     * Works with either auto-create OR auto-resolve being enabled
     */
    private void processAccount(LeagueAccount account) {
        String summonerId = account.getSummonerId();
        String userId = account.getUserId();

        // Skip accounts that have neither feature enabled
        if (!account.isAutoCreatePredictions() && !account.isAutoResolvePredictions()) {
            logger.debug("Account {} has neither auto-create nor auto-resolve enabled - skipping",
                    account.getSummonerName());
            return;
        }

        // Check current game status
        boolean isInGame = isInGame(account);
        GameTracker tracker = activeGames.get(summonerId);

        // Special case: if auto-create is disabled but we have an active prediction
        // that needs resolving
        if (!account.isAutoCreatePredictions() && account.isAutoResolvePredictions() && tracker != null) {
            // If player was in game but now isn't, resolve the prediction
            if (tracker.inGame && !isInGame) {
                logger.info("Auto-resolve: Summoner {} just finished a game - resolving prediction",
                        account.getSummonerName());
                resolveGamePrediction(account, tracker);
                tracker.inGame = false;
            }
            return;
        }

        // Special case: if auto-resolve is disabled but auto-create is enabled
        if (account.isAutoCreatePredictions() && !account.isAutoResolvePredictions()) {
            // If not tracking and in game, create a prediction
            if (tracker == null && isInGame) {
                logger.info("Auto-create: Summoner {} just entered a game - creating prediction",
                        account.getSummonerName());
                createGamePrediction(account);
            }
            return;
        }

        // If we get here, both auto-create and auto-resolve must be enabled somehow
        // (maybe through API or future UI changes)

        // Case 1: Not tracking and not in game - do nothing
        if (tracker == null && !isInGame) {
            logger.debug("Summoner {} is not in game and not being tracked", account.getSummonerName());
            return;
        }

        // Case 2: Not tracking but in game - create prediction
        if (tracker == null && isInGame) {
            logger.info("Summoner {} just entered a game - creating prediction", account.getSummonerName());
            createGamePrediction(account);
            return;
        }

        // Case 3: Tracking but not in game - resolve prediction
        if (tracker != null && !isInGame) {
            if (tracker.inGame) {
                logger.info("Summoner {} just finished a game - resolving prediction", account.getSummonerName());
                resolveGamePrediction(account, tracker);
                tracker.inGame = false;
            } else {
                // Already processed the game end, check for next game
                if (checkForNewGame(account)) {
                    logger.info("Summoner {} started a new game after previous one ended",
                            account.getSummonerName());
                    // Remove old tracker since we'll create a new one for the new game
                    activeGames.remove(summonerId);
                }
            }
            return;
        }

        // Case 4: Tracking and in game - update last check time
        if (tracker != null && isInGame) {
            logger.debug("Summoner {} is still in game", account.getSummonerName());
            account.setLastGameCheckTime(LocalDateTime.now());
            leagueAccountRepository.save(account);
        }
    }

    /**
     * Check if a summoner has started a new game after their previous game ended
     */
    private boolean checkForNewGame(LeagueAccount account) {
        // For mock mode: 10% chance of finding a new game after previous game ended
        boolean foundNewGame = Math.random() < 0.1;

        if (foundNewGame && account.isAutoCreatePredictions()) {
            createGamePrediction(account);
        }

        return foundNewGame;
    }

    /**
     * Check if summoner is in game
     * In mock mode, this simulates being in game
     */
    private boolean isInGame(LeagueAccount account) {
        String summonerId = account.getSummonerId();
        GameTracker tracker = activeGames.get(summonerId);

        // If we're tracking this summoner and they're marked as in game
        if (tracker != null && tracker.inGame) {
            // 90% chance to still be in game once we've detected a game
            // This simulates game duration
            return Math.random() < 0.9;
        }

        // For summoners not in a tracked game, 20% chance to enter a new game
        return Math.random() < 0.2;
    }

    private void createGamePrediction(LeagueAccount account) {
        try {
            String userId = account.getUserId();
            String summonerId = account.getSummonerId();
            String summonerName = account.getSummonerName();

            // Create a gameId
            String gameId = "MOCK-GAME-" + UUID.randomUUID().toString().substring(0, 8);

            // Check if account has an active template
            UUID templateId = account.getActiveTemplateId();

            // Default values if no template is found
            String title = "Will " + summonerName + " win their game?";
            List<String> outcomes = Arrays.asList("Win", "Loss");
            int duration = 30 * 60; // 30 minutes

            // If template ID exists, try to find the template
            if (templateId != null) {
                Optional<PredictionTemplate> templateOpt = templateRepository.findById(templateId);

                if (templateOpt.isPresent()) {
                    PredictionTemplate template = templateOpt.get();

                    // Use template values
                    title = template.getTitle().replace("{summonerName}", summonerName);
                    outcomes = Arrays.asList(template.getOutcome1(), template.getOutcome2());
                    duration = template.getDuration();

                    logger.info("Using template '{}' for prediction", template.getTitle());
                } else {
                    logger.warn("Template with ID {} not found, using defaults", templateId);
                }
            }

            // Create the prediction with template or default values
            Prediction prediction = predictionService.createPrediction(
                    userId,
                    title,
                    null, // No session
                    outcomes,
                    duration);

            // Start tracking this game
            if (prediction != null) {
                GameTracker tracker = new GameTracker(
                        summonerId,
                        userId,
                        gameId,
                        prediction.getId());

                activeGames.put(summonerId, tracker);

                logger.info("Created prediction {} for {} (game {})",
                        prediction.getId(), summonerName, gameId);
            }

        } catch (Exception e) {
            logger.error("Error creating game prediction: {}", e.getMessage(), e);
        }
    }

    /**
     * Resolve a prediction based on game outcome
     */
    private void resolveGamePrediction(LeagueAccount account, GameTracker tracker) {
        try {
            // Determine if the player won
            boolean playerWon = checkGameOutcome(account, tracker.gameId);

            // Get the prediction
            Prediction prediction = getPrediction(tracker.userId, tracker.predictionId);

            if (prediction != null && prediction.getOutcomes().size() >= 2) {
                // Get the outcome ID based on win/loss
                // Outcome[0] is "Win", Outcome[1] is "Loss"
                String outcomeId = playerWon ? prediction.getOutcomes().get(0).getId()
                        : prediction.getOutcomes().get(1).getId();

                // Resolve the prediction
                predictionService.endPrediction(
                        tracker.userId,
                        tracker.predictionId,
                        outcomeId);

                logger.info("Resolved prediction {} for {} with outcome: {}",
                        tracker.predictionId, account.getSummonerName(), playerWon ? "Win" : "Loss");
            }
        } catch (Exception e) {
            logger.error("Error resolving prediction: {}", e.getMessage(), e);
        }
    }

    /**
     * Check the outcome of a game
     * In mock mode, randomly determine win/loss
     */
    private boolean checkGameOutcome(LeagueAccount account, String gameId) {
        // In mock mode: 50% chance of winning
        return Math.random() < 0.5;
    }

    /**
     * Get a prediction by ID
     */
    private Prediction getPrediction(String userId, String predictionId) {
        try {
            List<Prediction> predictions = predictionService.getPredictions(userId, 50);

            for (Prediction prediction : predictions) {
                if (prediction.getId().equals(predictionId)) {
                    return prediction;
                }
            }
        } catch (Exception e) {
            logger.error("Error getting prediction: {}", e.getMessage(), e);
        }

        return null;
    }
}