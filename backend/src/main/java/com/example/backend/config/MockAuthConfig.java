package com.example.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import com.example.backend.service.MockTwitchAuthService;
import com.example.backend.service.TwitchAuthService;
import com.example.backend.model.TwitchToken;
import java.util.Optional;

/**
 * Configuration for enabling the mock authentication
 */
@Configuration
public class MockAuthConfig {
    /**
     * This bean creates a renamed alias for the MockTwitchAuthService with the same
     * name as the real service
     * When the mock profile is active, this bean will be used instead of the real
     * TwitchAuthService
     */
    @Bean
    @Profile("mock")
    @ConditionalOnProperty(name = "app.mock.enabled", havingValue = "true", matchIfMissing = false)
    public TwitchAuthService twitchAuthService(MockTwitchAuthService mockService) {
        // We'll create a proxy to the mock service that implements the
        // TwitchAuthService interface
        return new TwitchAuthService() {
            @Override
            public String getAuthorizationUrl() {
                return mockService.getAuthorizationUrl();
            }

            @Override
            public TwitchToken handleAuthCode(String code) {
                return mockService.handleAuthCode(code);
            }

            @Override
            public String getUserId(String accessToken) {
                return mockService.getUserId(accessToken);
            }

            @Override
            public Optional<TwitchToken> getTokenByUserId(String userId) {
                return mockService.getTokenByUserId(userId);
            }

            @Override
            public TwitchToken refreshToken(TwitchToken oldToken) {
                return mockService.refreshToken(oldToken);
            }

            @Override
            public String getValidAccessToken(String userId) {
                return mockService.getValidAccessToken(userId);
            }
        };
    }
}