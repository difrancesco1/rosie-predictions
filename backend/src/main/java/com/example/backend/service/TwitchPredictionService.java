package com.example.backend.service;

import com.example.backend.config.TwitchConfig;
import com.example.backend.model.Prediction;
import com.example.backend.model.PredictionOutcome;
import com.example.backend.model.PredictionSession;
import com.example.backend.repository.PredictionRepository;
import com.example.backend.repository.PredictionSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class TwitchPredictionService {

    @Autowired
    private TwitchConfig twitchConfig;

    @Autowired
    private TwitchAuthService authService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private PredictionRepository predictionRepository;

    @Autowired
    private PredictionSessionRepository sessionRepository;

    private static final String TWITCH_API_URL = "https://api.twitch.tv/helix";
    private static final int TWITCH_MAX_LIMIT = 25; // Twitch API maximum limit

    public List<Prediction> getPredictions(String userId, int limit) {
        String accessToken = authService.getValidAccessToken(userId);

        if (accessToken == null) {
            throw new RuntimeException("User not authenticated with Twitch");
        }

        HttpHeaders headers = createHeaders(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        // Use pagination to get more than 25 predictions if needed
        List<Prediction> allPredictions = new ArrayList<>();
        String cursor = null;
        int remaining = limit;

        do {
            // Cap the request limit to Twitch's maximum (25)
            int requestLimit = Math.min(remaining, TWITCH_MAX_LIMIT);

            // Build the URL with pagination if needed
            StringBuilder urlBuilder = new StringBuilder(TWITCH_API_URL)
                    .append("/predictions?broadcaster_id=")
                    .append(userId)
                    .append("&first=")
                    .append(requestLimit);

            if (cursor != null) {
                urlBuilder.append("&after=").append(cursor);
            }

            ResponseEntity<Map> response = restTemplate.exchange(
                    urlBuilder.toString(),
                    HttpMethod.GET,
                    entity,
                    Map.class);

            // Process the current page of results
            Map<String, Object> responseBody = response.getBody();
            List<Map<String, Object>> predictionData = (List<Map<String, Object>>) responseBody.get("data");

            if (predictionData != null && !predictionData.isEmpty()) {
                for (Map<String, Object> data : predictionData) {
                    Prediction prediction = mapToPrediction(data);
                    allPredictions.add(prediction);
                    predictionRepository.save(prediction);
                }

                // Update the remaining count
                remaining -= predictionData.size();

                // Check if we need to get more pages
                if (remaining > 0 && responseBody.containsKey("pagination")) {
                    Map<String, Object> pagination = (Map<String, Object>) responseBody.get("pagination");
                    cursor = pagination.containsKey("cursor") ? (String) pagination.get("cursor") : null;

                    // If no cursor, we've reached the end
                    if (cursor == null) {
                        break;
                    }
                } else {
                    break;
                }
            } else {
                break; // No data returned
            }
        } while (remaining > 0);

        return allPredictions;
    }

    public Prediction createPrediction(String userId, String title, String sessionId, List<String> outcomes,
            int predictionWindow) {
        String accessToken = authService.getValidAccessToken(userId);

        if (accessToken == null) {
            throw new RuntimeException("User not authenticated with Twitch");
        }

        if (outcomes.size() < 2 || outcomes.size() > 10) {
            throw new IllegalArgumentException("Predictions must have between 2 and 10 outcomes");
        }

        HttpHeaders headers = createHeaders(accessToken);
        headers.set("Content-Type", "application/json");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("broadcaster_id", userId);
        requestBody.put("title", title);
        requestBody.put("prediction_window", predictionWindow);

        List<Map<String, String>> outcomesList = new ArrayList<>();
        for (String outcome : outcomes) {
            Map<String, String> outcomeMap = new HashMap<>();
            outcomeMap.put("title", outcome);
            outcomesList.add(outcomeMap);
        }
        requestBody.put("outcomes", outcomesList);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                TWITCH_API_URL + "/predictions",
                HttpMethod.POST,
                entity,
                Map.class);

        Map<String, Object> responseData = response.getBody();
        List<Map<String, Object>> predictionData = (List<Map<String, Object>>) responseData.get("data");

        if (predictionData != null && !predictionData.isEmpty()) {
            Prediction prediction = mapToPrediction(predictionData.get(0));

            // Associate with session if provided
            if (sessionId != null && !sessionId.isEmpty()) {
                prediction.setSessionId(sessionId);
            }

            @SuppressWarnings("unchecked")
            Prediction savedPrediction = (Prediction) predictionRepository.save(prediction);
            return savedPrediction;
        }

        throw new RuntimeException("Failed to create prediction");
    }

    public Prediction endPrediction(String userId, String predictionId, String winningOutcomeId) {
        String accessToken = authService.getValidAccessToken(userId);

        if (accessToken == null) {
            throw new RuntimeException("User not authenticated with Twitch");
        }

        HttpHeaders headers = createHeaders(accessToken);
        headers.set("Content-Type", "application/json");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("broadcaster_id", userId);
        requestBody.put("id", predictionId);
        requestBody.put("status", "RESOLVED");
        requestBody.put("winning_outcome_id", winningOutcomeId);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                TWITCH_API_URL + "/predictions",
                HttpMethod.PATCH,
                entity,
                Map.class);

        Map<String, Object> responseData = response.getBody();
        List<Map<String, Object>> predictionData = (List<Map<String, Object>>) responseData.get("data");

        if (predictionData != null && !predictionData.isEmpty()) {
            Prediction prediction = mapToPrediction(predictionData.get(0));
            @SuppressWarnings("unchecked")
            Prediction savedPrediction = (Prediction) predictionRepository.save(prediction);
            return savedPrediction;
        }

        throw new RuntimeException("Failed to end prediction");
    }

    public Prediction cancelPrediction(String userId, String predictionId) {
        String accessToken = authService.getValidAccessToken(userId);

        if (accessToken == null) {
            throw new RuntimeException("User not authenticated with Twitch");
        }

        HttpHeaders headers = createHeaders(accessToken);
        headers.set("Content-Type", "application/json");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("broadcaster_id", userId);
        requestBody.put("id", predictionId);
        requestBody.put("status", "CANCELED");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                TWITCH_API_URL + "/predictions",
                HttpMethod.PATCH,
                entity,
                Map.class);

        Map<String, Object> responseData = response.getBody();
        List<Map<String, Object>> predictionData = (List<Map<String, Object>>) responseData.get("data");

        if (predictionData != null && !predictionData.isEmpty()) {
            Prediction prediction = mapToPrediction(predictionData.get(0));
            @SuppressWarnings("unchecked")
            Prediction savedPrediction = (Prediction) predictionRepository.save(prediction);
            return savedPrediction;
        }

        throw new RuntimeException("Failed to cancel prediction");
    }

    public PredictionSession createSession(String userId, String name, String description, String tags) {
        PredictionSession session = new PredictionSession();
        session.setId(UUID.randomUUID().toString());
        session.setName(name);
        session.setBroadcasterId(userId);
        session.setStartedAt(LocalDateTime.now());
        session.setStatus("ACTIVE");
        session.setDescription(description);
        session.setTags(tags);

        @SuppressWarnings("unchecked")
        PredictionSession savedSession = (PredictionSession) sessionRepository.save(session);
        return savedSession;
    }

    public PredictionSession endSession(String sessionId) {
        Optional<PredictionSession> sessionOpt = sessionRepository.findById(sessionId);

        if (sessionOpt.isPresent()) {
            PredictionSession session = sessionOpt.get();
            session.setStatus("COMPLETED");
            session.setEndedAt(LocalDateTime.now());

            @SuppressWarnings("unchecked")
            PredictionSession savedSession = (PredictionSession) sessionRepository.save(session);
            return savedSession;
        }

        throw new RuntimeException("Session not found");
    }

    public List<PredictionSession> getActiveSessions(String userId) {
        return sessionRepository.findByBroadcasterIdAndStatusOrderByStartedAtDesc(userId, "ACTIVE");
    }

    public List<PredictionSession> getAllSessions(String userId) {
        return sessionRepository.findByBroadcasterIdOrderByStartedAtDesc(userId);
    }

    public List<Prediction> getSessionPredictions(String userId, String sessionId) {
        return predictionRepository.findByBroadcasterIdAndSessionIdOrderByCreatedAtDesc(userId, sessionId);
    }

    private HttpHeaders createHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("Client-Id", twitchConfig.getClientId());
        return headers;
    }

    private Prediction mapToPrediction(Map<String, Object> data) {
        Prediction prediction = new Prediction();
        prediction.setId((String) data.get("id"));
        prediction.setBroadcasterId((String) data.get("broadcaster_id"));
        prediction.setTitle((String) data.get("title"));

        String status = (String) data.get("status");
        prediction.setStatus(status);

        // Parse dates
        String createdAt = (String) data.get("created_at");
        if (createdAt != null) {
            prediction.setCreatedAt(parseDate(createdAt));
        }

        String endedAt = (String) data.get("ended_at");
        if (endedAt != null) {
            prediction.setEndedAt(parseDate(endedAt));
        }

        String lockedAt = (String) data.get("locked_at");
        if (lockedAt != null) {
            prediction.setLockedAt(parseDate(lockedAt));
        }

        prediction.setWinningOutcomeId((String) data.get("winning_outcome_id"));

        // Map outcomes
        List<Map<String, Object>> outcomes = (List<Map<String, Object>>) data.get("outcomes");
        if (outcomes != null) {
            List<PredictionOutcome> outcomeList = new ArrayList<>();

            for (Map<String, Object> outcomeData : outcomes) {
                PredictionOutcome outcome = new PredictionOutcome();
                outcome.setId((String) outcomeData.get("id"));
                outcome.setTitle((String) outcomeData.get("title"));
                outcome.setColor((String) outcomeData.get("color"));

                // Some fields might be missing in some API responses
                if (outcomeData.containsKey("users")) {
                    outcome.setUsers(((Number) outcomeData.get("users")).intValue());
                }

                if (outcomeData.containsKey("channel_points")) {
                    outcome.setChannelPoints(((Number) outcomeData.get("channel_points")).intValue());
                }

                outcomeList.add(outcome);
            }

            prediction.setOutcomes(outcomeList);
        }

        return prediction;
    }

    private LocalDateTime parseDate(String dateString) {
        // Parse Twitch API date format (ISO-8601)
        return LocalDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME);
    }
}