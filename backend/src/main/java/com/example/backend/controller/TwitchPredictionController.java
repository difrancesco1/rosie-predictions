package com.example.backend.controller;

import com.example.backend.model.Prediction;
import com.example.backend.model.PredictionSession;
import com.example.backend.service.TwitchPredictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/predictions")
public class TwitchPredictionController {

    @Autowired
    private TwitchPredictionService predictionService;

    /**
     * Get recent predictions for a user
     */
    @GetMapping("/{userId}")
    public ResponseEntity<List<Prediction>> getPredictions(
            @PathVariable String userId,
            @RequestParam(defaultValue = "20") int limit) {
        List<Prediction> predictions = predictionService.getPredictions(userId, limit);
        return ResponseEntity.ok(predictions);
    }

    /**
     * Create a new prediction
     */
    @PostMapping("/{userId}")
    public ResponseEntity<Prediction> createPrediction(
            @PathVariable String userId,
            @RequestBody CreatePredictionRequest request) {
        Prediction prediction = predictionService.createPrediction(
                userId,
                request.getTitle(),
                request.getSessionId(),
                request.getOutcomes(),
                request.getPredictionWindow());
        return ResponseEntity.ok(prediction);
    }

    /**
     * End a prediction with a winning outcome
     */
    @PatchMapping("/{userId}/{predictionId}/resolve")
    public ResponseEntity<Prediction> resolvePrediction(
            @PathVariable String userId,
            @PathVariable String predictionId,
            @RequestBody ResolvePredictionRequest request) {
        Prediction prediction = predictionService.endPrediction(
                userId,
                predictionId,
                request.getWinningOutcomeId());
        return ResponseEntity.ok(prediction);
    }

    /**
     * Cancel a prediction
     */
    @PatchMapping("/{userId}/{predictionId}/cancel")
    public ResponseEntity<Prediction> cancelPrediction(
            @PathVariable String userId,
            @PathVariable String predictionId) {
        Prediction prediction = predictionService.cancelPrediction(userId, predictionId);
        return ResponseEntity.ok(prediction);
    }

    /**
     * Create a new session for grouping predictions
     */
    @PostMapping("/{userId}/sessions")
    public ResponseEntity<PredictionSession> createSession(
            @PathVariable String userId,
            @RequestBody CreateSessionRequest request) {
        PredictionSession session = predictionService.createSession(
                userId,
                request.getName(),
                request.getDescription(),
                request.getTags());
        return ResponseEntity.ok(session);
    }

    /**
     * End an active session
     */
    @PatchMapping("/{userId}/sessions/{sessionId}/end")
    public ResponseEntity<PredictionSession> endSession(
            @PathVariable String userId,
            @PathVariable String sessionId) {
        PredictionSession session = predictionService.endSession(sessionId);
        return ResponseEntity.ok(session);
    }

    /**
     * Get all active sessions for a user
     */
    @GetMapping("/{userId}/sessions/active")
    public ResponseEntity<List<PredictionSession>> getActiveSessions(
            @PathVariable String userId) {
        List<PredictionSession> sessions = predictionService.getActiveSessions(userId);
        return ResponseEntity.ok(sessions);
    }

    /**
     * Get all sessions for a user
     */
    @GetMapping("/{userId}/sessions")
    public ResponseEntity<List<PredictionSession>> getAllSessions(
            @PathVariable String userId) {
        List<PredictionSession> sessions = predictionService.getAllSessions(userId);
        return ResponseEntity.ok(sessions);
    }

    /**
     * Get all predictions for a specific session
     */
    @GetMapping("/{userId}/sessions/{sessionId}/predictions")
    public ResponseEntity<List<Prediction>> getSessionPredictions(
            @PathVariable String userId,
            @PathVariable String sessionId) {
        List<Prediction> predictions = predictionService.getSessionPredictions(userId, sessionId);
        return ResponseEntity.ok(predictions);
    }

    /**
     * Get statistics about predictions and sessions
     */
    @GetMapping("/{userId}/stats")
    public ResponseEntity<Map<String, Object>> getStats(@PathVariable String userId) {
        Map<String, Object> stats = new HashMap<>();

        List<Prediction> predictions = predictionService.getPredictions(userId, 100);
        List<PredictionSession> sessions = predictionService.getAllSessions(userId);

        stats.put("totalPredictions", predictions.size());
        stats.put("totalSessions", sessions.size());
        stats.put("activeSessions", predictionService.getActiveSessions(userId).size());

        // Count predictions by status
        Map<String, Integer> predictionsByStatus = new HashMap<>();
        for (Prediction prediction : predictions) {
            String status = prediction.getStatus();
            predictionsByStatus.put(status, predictionsByStatus.getOrDefault(status, 0) + 1);
        }
        stats.put("predictionsByStatus", predictionsByStatus);

        return ResponseEntity.ok(stats);
    }

    // Request/Response DTO classes

    public static class CreatePredictionRequest {
        private String title;
        private String sessionId;
        private List<String> outcomes;
        private int predictionWindow;

        // Getters and setters
        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        public List<String> getOutcomes() {
            return outcomes;
        }

        public void setOutcomes(List<String> outcomes) {
            this.outcomes = outcomes;
        }

        public int getPredictionWindow() {
            return predictionWindow;
        }

        public void setPredictionWindow(int predictionWindow) {
            this.predictionWindow = predictionWindow;
        }
    }

    public static class ResolvePredictionRequest {
        private String winningOutcomeId;

        public String getWinningOutcomeId() {
            return winningOutcomeId;
        }

        public void setWinningOutcomeId(String winningOutcomeId) {
            this.winningOutcomeId = winningOutcomeId;
        }
    }

    public static class CreateSessionRequest {
        private String name;
        private String description;
        private String tags;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getTags() {
            return tags;
        }

        public void setTags(String tags) {
            this.tags = tags;
        }
    }
}