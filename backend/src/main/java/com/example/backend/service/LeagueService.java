package com.example.backend.service;

import com.example.backend.config.LeagueConfig;
import com.example.backend.model.LeagueAccount;
import com.example.backend.repository.LeagueAccountRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class LeagueService {
    private static final Logger logger = LoggerFactory.getLogger(LeagueService.class);
    private static final String SUMMONER_V4_PATH = "/lol/summoner/v4/summoners/by-name/";
    private static final String MATCH_V5_PATH = "/lol/match/v5/matches/by-puuid/";
    private static final String MATCH_DETAIL_PATH = "/lol/match/v5/matches/";

    private final LeagueConfig leagueConfig;
    private final RestTemplate restTemplate;
    private final LeagueAccountRepository leagueAccountRepository;
    private final TwitchPredictionService predictionService;
    private final ObjectMapper objectMapper;

    @Autowired
    public LeagueService(LeagueConfig leagueConfig, RestTemplate restTemplate,
            LeagueAccountRepository leagueAccountRepository,
            TwitchPredictionService predictionService) {
        this.leagueConfig = leagueConfig;
        this.restTemplate = restTemplate;
        this.leagueAccountRepository = leagueAccountRepository;
        this.predictionService = predictionService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Connect a Twitch user's account to their League of Legends summoner name
     */
    public LeagueAccount connectAccount(String userId, String summonerName) throws IOException {
        // Check if account already exists
        Optional<LeagueAccount> existingAccount = leagueAccountRepository.findBySummonerName(summonerName);
        if (existingAccount.isPresent()) {
            throw new IOException("This summoner is already connected to an account");
        }

        // Parse Riot ID
        String riotId = summonerName;
        String tagLine = "";
        if (summonerName.contains("#")) {
            String[] parts = summonerName.split("#", 2);
            riotId = parts[0];
            tagLine = parts[1];
            logger.info("Parsed riotId: {}, tagLine: {}", riotId, tagLine);
        }

        try {
            // First deactivate any existing active accounts
            deactivateAllAccounts(userId);

            // For modern Riot IDs, use the account-v1 API
            String encodedRiotId = URLEncoder.encode(riotId, StandardCharsets.UTF_8);
            String accountApiUrl = "https://americas.api.riotgames.com/riot/account/v1/accounts/by-riot-id/"
                    + encodedRiotId + "/" + URLEncoder.encode(tagLine, StandardCharsets.UTF_8);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Riot-Token", leagueConfig.getApiKey());

            // Make the request to get the PUUID
            ResponseEntity<String> response = restTemplate.exchange(
                    accountApiUrl, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            // Parse the response
            JsonNode accountData = objectMapper.readTree(response.getBody());
            String puuid = accountData.get("puuid").asText();

            // Now get summoner data using the PUUID
            String summonerByPuuidUrl = leagueConfig.getNaBaseUrl() + "/lol/summoner/v4/summoners/by-puuid/" + puuid;

            ResponseEntity<String> summonerResponse = restTemplate.exchange(
                    summonerByPuuidUrl, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            JsonNode summonerData = objectMapper.readTree(summonerResponse.getBody());

            // Create and save the account
            LeagueAccount account = new LeagueAccount(userId, summonerName);
            account.setPuuid(puuid);
            account.setSummonerId(summonerData.get("id").asText());
            account.setRegion("na1"); // Default to NA
            account.setActive(true);

            return leagueAccountRepository.save(account);
        } catch (Exception e) {
            logger.error("Error connecting to Riot API: {}", e.getMessage());

            // For testing/debugging, you can use this fallback for development
            /*
             * LeagueAccount account = new LeagueAccount(userId, summonerName);
             * account.setSummonerId("test-summoner-id");
             * account.setPuuid("test-puuid");
             * account.setRegion("na1");
             * account.setActive(true);
             * return leagueAccountRepository.save(account);
             */

            throw new IOException("Error communicating with Riot API: " + e.getMessage());
        }
    }

    /**
     * Get all accounts for a user
     */
    public List<LeagueAccount> getAllAccountsByUserId(String userId) {
        return leagueAccountRepository.findByUserId(userId);
    }

    /**
     * Get active account for a user
     */
    public Optional<LeagueAccount> getActiveAccountByUserId(String userId) {
        return leagueAccountRepository.findByUserIdAndIsActiveTrue(userId);
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
     * Deactivate all accounts for a user
     */
    private void deactivateAllAccounts(String userId) {
        List<LeagueAccount> accounts = leagueAccountRepository.findByUserId(userId);
        for (LeagueAccount account : accounts) {
            account.setActive(false);
            leagueAccountRepository.save(account);
        }
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
     * Delete a user's League account connection
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
     * Runs on a schedule defined by polling-interval property
     */
    @Scheduled(fixedDelayString = "${league.polling-interval:60}000")
    public void checkForGames() {
        logger.info("Checking for League games...");

        // Find accounts with auto features enabled (only active accounts)
        List<LeagueAccount> autoCreateAccounts = leagueAccountRepository
                .findByAutoCreatePredictionsTrueAndIsActiveTrue();
        List<LeagueAccount> autoResolveAccounts = leagueAccountRepository
                .findByAutoResolvePredictionsTrueAndIsActiveTrue();

        // Check for active games and create predictions
        for (LeagueAccount account : autoCreateAccounts) {
            try {
                checkAndCreatePrediction(account);
            } catch (Exception e) {
                logger.error("Error checking games for user {}: {}", account.getUserId(), e.getMessage());
            }
        }

        // Check for completed games and resolve predictions
        for (LeagueAccount account : autoResolveAccounts) {
            try {
                checkAndResolvePrediction(account);
            } catch (Exception e) {
                logger.error("Error resolving predictions for user {}: {}", account.getUserId(), e.getMessage());
            }
        }
    }

    /**
     * Check if user is in a game and create a prediction
     */
    private void checkAndCreatePrediction(LeagueAccount account) {
        // Implementation would check if user is in an active game
        // If so, create a prediction with appropriate outcomes

        // This is a placeholder for actual implementation
        // You would need to use the Riot Games API to check for active games
    }

    /**
     * Check for recently completed games and resolve predictions
     */
    private void checkAndResolvePrediction(LeagueAccount account) throws IOException {
        // Get recent matches
        String matchesUrl = leagueConfig.getAmericasBaseUrl() + MATCH_V5_PATH +
                account.getPuuid() + "/ids?start=0&count=1";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Riot-Token", leagueConfig.getApiKey());

        ResponseEntity<String> response = restTemplate.exchange(
                matchesUrl, HttpMethod.GET, new HttpEntity<>(headers), String.class);

        JsonNode matchesData = objectMapper.readTree(response.getBody());

        // Check if there are matches and the most recent one is newer than last check
        if (matchesData.isArray() && matchesData.size() > 0) {
            String matchId = matchesData.get(0).asText();

            // Get match details
            String matchDetailUrl = leagueConfig.getAmericasBaseUrl() + MATCH_DETAIL_PATH + matchId;
            ResponseEntity<String> matchResponse = restTemplate.exchange(
                    matchDetailUrl, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            JsonNode matchDetail = objectMapper.readTree(matchResponse.getBody());

            // Find the player in the match
            JsonNode participants = matchDetail.get("info").get("participants");
            for (JsonNode participant : participants) {
                String puuid = participant.get("puuid").asText();

                if (puuid.equals(account.getPuuid())) {
                    boolean won = participant.get("win").asBoolean();

                    // Find active predictions for this user
                    // Get a reasonable number of predictions and filter for active ones
                    List<com.example.backend.model.Prediction> predictions = predictionService
                            .getPredictions(account.getUserId(), 10);

                    // Filter for active predictions
                    List<com.example.backend.model.Prediction> activePredictions = predictions.stream()
                            .filter(p -> "ACTIVE".equals(p.getStatus()))
                            .toList();

                    if (!activePredictions.isEmpty()) {
                        com.example.backend.model.Prediction prediction = activePredictions.get(0);

                        // Determine which outcome ID corresponds to win/loss
                        String winningOutcomeId = null;
                        for (var outcome : prediction.getOutcomes()) {
                            if ((won && outcome.getTitle().toLowerCase().contains("win")) ||
                                    (!won && outcome.getTitle().toLowerCase().contains("lose"))) {
                                winningOutcomeId = outcome.getId();
                                break;
                            }
                        }

                        if (winningOutcomeId != null) {
                            // Resolve the prediction
                            predictionService.endPrediction(account.getUserId(), prediction.getId(), winningOutcomeId);
                            logger.info("Resolved prediction {} for user {} with outcome {}",
                                    prediction.getId(), account.getUserId(), winningOutcomeId);
                        }
                    }

                    // Update last check time
                    account.setLastGameCheckTime(LocalDateTime.now());
                    leagueAccountRepository.save(account);

                    break;
                }
            }
        }
    }
}