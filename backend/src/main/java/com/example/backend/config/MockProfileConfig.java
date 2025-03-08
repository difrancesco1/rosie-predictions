package com.example.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import com.example.backend.service.MockTwitchPredictionService;
import com.example.backend.service.TwitchPredictionService;
import com.example.backend.model.Prediction;
import com.example.backend.model.PredictionSession;
import java.util.List;

/**
 * Configuration for enabling the mock profile
 * This sets up the application to use the mock service instead of the real one
 * when enabled
 */
@Configuration
public class MockProfileConfig {
    /**
     * This bean creates a renamed alias for the MockTwitchPredictionService with
     * the same name as the real service
     * When the mock profile is active, this bean will be used instead of the real
     * TwitchPredictionService
     */
    @Bean
    @Profile("mock")
    @ConditionalOnProperty(name = "app.mock.enabled", havingValue = "true", matchIfMissing = false)
    public TwitchPredictionService twitchPredictionService(MockTwitchPredictionService mockService) {
        // We'll create a proxy to the mock service that implements the
        // TwitchPredictionService interface
        return new TwitchPredictionService() {
            @Override
            public List<Prediction> getPredictions(String userId, int limit) {
                return mockService.getPredictions(userId, limit);
            }

            @Override
            public Prediction createPrediction(String userId, String title, String sessionId, List<String> outcomes,
                    int predictionWindow) {
                return mockService.createPrediction(userId, title, sessionId, outcomes, predictionWindow);
            }

            @Override
            public Prediction endPrediction(String userId, String predictionId, String winningOutcomeId) {
                return mockService.endPrediction(userId, predictionId, winningOutcomeId);
            }

            @Override
            public Prediction cancelPrediction(String userId, String predictionId) {
                return mockService.cancelPrediction(userId, predictionId);
            }

            @Override
            public PredictionSession createSession(String userId, String name, String description, String tags) {
                return mockService.createSession(userId, name, description, tags);
            }

            @Override
            public PredictionSession endSession(String sessionId) {
                return mockService.endSession(sessionId);
            }

            @Override
            public List<PredictionSession> getActiveSessions(String userId) {
                return mockService.getActiveSessions(userId);
            }

            @Override
            public List<PredictionSession> getAllSessions(String userId) {
                return mockService.getAllSessions(userId);
            }

            @Override
            public List<Prediction> getSessionPredictions(String userId, String sessionId) {
                return mockService.getSessionPredictions(userId, sessionId);
            }
        };
    }
}