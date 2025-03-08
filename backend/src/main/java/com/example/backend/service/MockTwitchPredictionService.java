package com.example.backend.service;

import com.example.backend.model.Prediction;
import com.example.backend.model.PredictionOutcome;
import com.example.backend.model.PredictionSession;
import com.example.backend.repository.PredictionRepository;
import com.example.backend.repository.PredictionSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Mock implementation of the Twitch Prediction Service
 * This service provides simulated responses for prediction operations
 * without requiring a live Twitch stream or actually calling the Twitch API
 */
@Service
@Profile("mock") // This service will only be active when the "mock" profile is enabled
public class MockTwitchPredictionService {

    @Autowired
    private PredictionRepository predictionRepository;

    @Autowired
    private PredictionSessionRepository sessionRepository;

    // Store for active mock predictions
    private final Map<String, Prediction> activePredictions = new HashMap<>();

    public List<Prediction> getPredictions(String userId, int limit) {
        // Only return stored predictions without generating mock ones
        List<Prediction> storedPredictions = predictionRepository.findByBroadcasterIdOrderByCreatedAtDesc(userId);

        // Return the stored predictions, limited to the requested count
        return storedPredictions.size() <= limit ? storedPredictions : storedPredictions.subList(0, limit);
    }

    public Prediction createPrediction(String userId, String title, String sessionId, List<String> outcomes,
            int predictionWindow) {

        if (outcomes.size() < 2 || outcomes.size() > 10) {
            throw new IllegalArgumentException("Predictions must have between 2 and 10 outcomes");
        }

        // Create a mock prediction
        Prediction prediction = new Prediction();
        prediction.setId("mock-" + UUID.randomUUID().toString());
        prediction.setBroadcasterId(userId);
        prediction.setTitle(title);
        prediction.setStatus("ACTIVE");
        prediction.setCreatedAt(LocalDateTime.now());

        // Mock prediction window - simulate auto lock after predictionWindow seconds
        LocalDateTime lockTime = LocalDateTime.now().plusSeconds(predictionWindow);
        prediction.setLockedAt(lockTime);

        // Create mock outcomes
        List<PredictionOutcome> outcomesList = new ArrayList<>();
        for (int i = 0; i < outcomes.size(); i++) {
            PredictionOutcome outcome = new PredictionOutcome();
            outcome.setId("outcome-" + UUID.randomUUID().toString());
            outcome.setTitle(outcomes.get(i));
            outcome.setColor(i % 2 == 0 ? "BLUE" : "PINK");
            // Start with 0 points and users
            outcome.setUsers(0);
            outcome.setChannelPoints(0);
            outcomesList.add(outcome);
        }
        prediction.setOutcomes(outcomesList);

        // Associate with session if provided
        if (sessionId != null && !sessionId.isEmpty()) {
            prediction.setSessionId(sessionId);
        }

        // Simulate some users betting
        simulateUsers(prediction);

        // Store in our active predictions map
        activePredictions.put(prediction.getId(), prediction);

        // Save to DB
        return predictionRepository.save(prediction);
    }

    public Prediction endPrediction(String userId, String predictionId, String winningOutcomeId) {
        // Get the prediction - either from our active map or from the repository
        Prediction prediction = activePredictions.getOrDefault(
                predictionId,
                predictionRepository.findById(predictionId)
                        .orElseThrow(() -> new RuntimeException("Prediction not found with ID: " + predictionId)));

        // Verify broadcaster ID
        if (!prediction.getBroadcasterId().equals(userId)) {
            throw new RuntimeException("Prediction does not belong to user: " + userId);
        }

        // Verify the winning outcome exists
        boolean validOutcome = prediction.getOutcomes().stream()
                .anyMatch(outcome -> outcome.getId().equals(winningOutcomeId));

        if (!validOutcome) {
            throw new RuntimeException("Invalid outcome ID: " + winningOutcomeId);
        }

        // Update the prediction
        prediction.setStatus("RESOLVED");
        prediction.setEndedAt(LocalDateTime.now());
        prediction.setWinningOutcomeId(winningOutcomeId);

        // Remove from active predictions
        activePredictions.remove(predictionId);

        // Save to repository
        return predictionRepository.save(prediction);
    }

    public Prediction cancelPrediction(String userId, String predictionId) {
        // Get the prediction - either from our active map or from the repository
        Prediction prediction = activePredictions.getOrDefault(
                predictionId,
                predictionRepository.findById(predictionId)
                        .orElseThrow(() -> new RuntimeException("Prediction not found with ID: " + predictionId)));

        // Verify broadcaster ID
        if (!prediction.getBroadcasterId().equals(userId)) {
            throw new RuntimeException("Prediction does not belong to user: " + userId);
        }

        // Update the prediction
        prediction.setStatus("CANCELED");
        prediction.setEndedAt(LocalDateTime.now());

        // Remove from active predictions
        activePredictions.remove(predictionId);

        // Save to repository
        return predictionRepository.save(prediction);
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

        return sessionRepository.save(session);
    }

    public PredictionSession endSession(String sessionId) {
        Optional<PredictionSession> sessionOpt = sessionRepository.findById(sessionId);

        if (sessionOpt.isPresent()) {
            PredictionSession session = sessionOpt.get();
            session.setStatus("COMPLETED");
            session.setEndedAt(LocalDateTime.now());

            return sessionRepository.save(session);
        }

        throw new RuntimeException("Session not found with ID: " + sessionId);
    }

    public List<PredictionSession> getActiveSessions(String userId) {
        return sessionRepository.findByBroadcasterIdAndStatusOrderByStartedAtDesc(userId, "ACTIVE");
    }

    public List<PredictionSession> getAllSessions(String userId) {
        List<PredictionSession> storedSessions = sessionRepository.findByBroadcasterIdOrderByStartedAtDesc(userId);

        // If no sessions, create a default one
        if (storedSessions.isEmpty()) {
            PredictionSession defaultSession = createSession(
                    userId,
                    "Mock Stream Session",
                    "Automatically created mock session",
                    "mock,test");
            storedSessions = Collections.singletonList(defaultSession);
        }

        return storedSessions;
    }

    public List<Prediction> getSessionPredictions(String userId, String sessionId) {
        return predictionRepository.findByBroadcasterIdAndSessionIdOrderByCreatedAtDesc(userId, sessionId);
    }

    // Helper method for simulating user bets
    private void simulateUsers(Prediction prediction) {
        // Get all outcomes
        List<PredictionOutcome> outcomes = prediction.getOutcomes();
        if (outcomes == null || outcomes.isEmpty()) {
            return;
        }

        // Random number of users for each outcome
        Random random = new Random();
        for (PredictionOutcome outcome : outcomes) {
            int users = random.nextInt(41) + 10; // 10-50 users
            int avgBet = random.nextInt(501) + 500; // 500-1000 points per user on average

            outcome.setUsers(users);
            outcome.setChannelPoints(users * avgBet);
        }
    }
}