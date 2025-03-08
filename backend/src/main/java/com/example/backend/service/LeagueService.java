package com.example.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.backend.model.LeagueAccount;
import com.example.backend.model.Prediction;
import com.example.backend.repository.LeagueAccountRepository;
import com.example.backend.config.LeagueConfig;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Service for interacting with the Riot League of Legends API
 */
@Service
public class LeagueService {
    private static final Logger logger = LoggerFactory.getLogger(LeagueService.class);

    private final LeagueAccountRepository leagueAccountRepository;
    private final TwitchPredictionService predictionService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final LeagueConfig leagueConfig;

    @Autowired
    public LeagueService(LeagueAccountRepository leagueAccountRepository,
            TwitchPredictionService predictionService,
            RestTemplate restTemplate,
            LeagueConfig leagueConfig) {
        this.leagueAccountRepository = leagueAccountRepository;
        this.predictionService = predictionService;
        this.restTemplate = restTemplate;
        this.leagueConfig = leagueConfig;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Save or update a League account
     */
    public LeagueAccount saveAccount(LeagueAccount account) {
        return leagueAccountRepository.save(account);
    }

    /**
     * Get all League accounts for a user
     */
    public List<LeagueAccount> getAccountsByUserId(String userId) {
        return leagueAccountRepository.findAllByUserId(userId);
    }

    /**
     * Delete a League account
     */
    public void deleteAccount(UUID id) {
        leagueAccountRepository.deleteById(id);
    }

    /**
     * Update account settings
     */
    public LeagueAccount updateAccountSettings(UUID id, boolean autoCreatePredictions, boolean autoResolvePredictions) {
        LeagueAccount account = leagueAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        account.setAutoCreatePredictions(autoCreatePredictions);
        account.setAutoResolvePredictions(autoResolvePredictions);

        return leagueAccountRepository.save(account);
    }

    /**
     * Check for new games and create/resolve predictions
     */
    @Scheduled(fixedDelayString = "${league.polling-interval:60}000")
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
                checkAndCreatePrediction(account);
            } catch (Exception e) {
                logger.error("Error checking games for account {}: {}", account.getSummonerName(), e.getMessage());
            }
        }

        // Check for completed games and resolve predictions
        for (LeagueAccount account : autoResolveAccounts) {
            try {
                logger.info("Checking for completed games for account: {}", account.getSummonerName());
                checkAndResolvePrediction(account);
            } catch (Exception e) {
                logger.error("Error resolving predictions for account {}: {}", account.getSummonerName(),
                        e.getMessage());
            }
        }
    }

    /**
     * Check if user is in a game and create a prediction
     */
    private void checkAndCreatePrediction(LeagueAccount account) {
        try {
            logger.info("Checking if player {} is in a game", account.getSummonerName());

            // Build the URL for checking active game
            String spectatorUrl = leagueConfig.getNaBaseUrl() + "/lol/spectator/v4/active-games/by-summoner/"
                    + account.getSummonerId();

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Riot-Token", leagueConfig.getApiKey());

            // Make API call to check if player is in an active game
            ResponseEntity<String> response;
            try {
                response = restTemplate.exchange(
                        spectatorUrl, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            } catch (HttpClientErrorException.NotFound e) {
                // No active game found (404 is expected when not in game)
                return;
            }

            // If we get here, player is in an active game
            JsonNode gameData = objectMapper.readTree(response.getBody());
            long gameId = gameData.get("gameId").asLong();
            String gameMode = gameData.get("gameMode").asText();

            // Check if we already have a prediction running for this game
            List<Prediction> activePredictions = predictionService.getPredictions(account.getUserId(), 5);
            boolean predictionExists = activePredictions.stream()
                    .filter(p -> "ACTIVE".equals(p.getStatus()))
                    .anyMatch(p -> {
                        // Check if prediction title contains this game ID (we'll include it for
                        // tracking)
                        return p.getTitle().contains("Game #" + gameId);
                    });

            if (predictionExists) {
                logger.info("Prediction already exists for game {}", gameId);
                return;
            }

            // Create a new prediction for this game
            logger.info("Creating prediction for {} in game {}", account.getSummonerName(), gameId);

            // Create outcomes - Win or Lose
            List<String> outcomes = new ArrayList<>();
            outcomes.add("Win");
            outcomes.add("Lose");

            // Create prediction title
            String title = account.getSummonerName() + " - Game #" + gameId;

            // Call the TwitchPredictionService to create the prediction
            Prediction prediction = predictionService.createPrediction(
                    account.getUserId(),
                    title,
                    null, // No session ID
                    outcomes,
                    300 // 5 minutes prediction window
            );

            logger.info("Successfully created prediction {} for game {}", prediction.getId(), gameId);
        } catch (Exception e) {
            logger.error("Error checking game status for {}: {}", account.getSummonerName(), e.getMessage(), e);
        }
    }

    /**
     * Check for recently completed games and resolve predictions
     */
    private void checkAndResolvePrediction(LeagueAccount account) {
        try {
            // Get active predictions for this user
            List<Prediction> activePredictions = predictionService.getPredictions(account.getUserId(), 5);

            // Filter for active predictions only
            List<Prediction> ongoingPredictions = activePredictions.stream()
                    .filter(p -> "ACTIVE".equals(p.getStatus()))
                    .toList();

            if (ongoingPredictions.isEmpty()) {
                return; // No active predictions to resolve
            }

            // Get recent matches
            String matchesUrl = leagueConfig.getAmericasBaseUrl() + "/lol/match/v5/matches/by-puuid/" +
                    account.getPuuid() + "/ids?start=0&count=5";

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Riot-Token", leagueConfig.getApiKey());

            ResponseEntity<String> response = restTemplate.exchange(
                    matchesUrl, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            JsonNode matchesData = objectMapper.readTree(response.getBody());

            // Check if there are matches
            if (!matchesData.isArray() || matchesData.size() == 0) {
                return;
            }

            // Get most recent match details
            String matchId = matchesData.get(0).asText();

            // Check if this match is newer than last check time
            LocalDateTime lastCheck = account.getLastGameCheckTime();

            // Get match details
            String matchDetailUrl = leagueConfig.getAmericasBaseUrl() + "/lol/match/v5/matches/" + matchId;
            ResponseEntity<String> matchResponse = restTemplate.exchange(
                    matchDetailUrl, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            JsonNode matchDetail = objectMapper.readTree(matchResponse.getBody());

            // Get match end time
            long gameEndTimestamp = matchDetail.get("info").get("gameEndTimestamp").asLong();
            LocalDateTime gameEndTime = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(gameEndTimestamp), ZoneId.systemDefault());

            // If game ended before last check, we've already processed it
            if (lastCheck != null && gameEndTime.isBefore(lastCheck)) {
                return;
            }

            // Find player in the match
            JsonNode participants = matchDetail.get("info").get("participants");
            for (JsonNode participant : participants) {
                String puuid = participant.get("puuid").asText();

                if (puuid.equals(account.getPuuid())) {
                    boolean won = participant.get("win").asBoolean();

                    // Find matching prediction
                    for (Prediction prediction : ongoingPredictions) {
                        // Extract game ID from prediction title
                        String title = prediction.getTitle();
                        if (title.contains("Game #")) {
                            // Update prediction based on win/loss
                            String winningOutcomeId = getWinningOutcomeId(prediction, won);

                            if (winningOutcomeId != null) {
                                predictionService.endPrediction(account.getUserId(), prediction.getId(),
                                        winningOutcomeId);
                                logger.info("Resolved prediction {} for game {} with result: {}",
                                        prediction.getId(), matchId, won ? "Win" : "Loss");
                            }
                        }
                    }

                    // Update last check time
                    account.setLastGameCheckTime(LocalDateTime.now());
                    leagueAccountRepository.save(account);

                    break;
                }
            }
        } catch (Exception e) {
            logger.error("Error resolving prediction for {}: {}", account.getSummonerName(), e.getMessage(), e);
        }
    }

    /**
     * Helper method to find winning outcome ID based on win/loss
     */
    private String getWinningOutcomeId(Prediction prediction, boolean won) {
        for (var outcome : prediction.getOutcomes()) {
            String title = outcome.getTitle().toLowerCase();
            if ((won && title.contains("win")) || (!won && title.contains("lose"))) {
                return outcome.getId();
            }
        }
        return null;
    }
}