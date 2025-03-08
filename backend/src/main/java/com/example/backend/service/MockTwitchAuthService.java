package com.example.backend.service;

import com.example.backend.model.TwitchToken;
import com.example.backend.repository.TwitchTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * A mock implementation of the Twitch authentication service
 */
@Service
@Profile("mock")
public class MockTwitchAuthService {

    @Autowired
    private TwitchTokenRepository tokenRepository;

    public String getAuthorizationUrl() {
        // Return a mock URL - in mock mode, the frontend would still call this but not
        // actually redirect
        return "https://mock-twitch-auth.example.com/authorize";
    }

    public TwitchToken handleAuthCode(String code) {
        // Generate a random user ID for mock auth
        String userId = "mock-user-" + UUID.randomUUID().toString().substring(0, 8);

        // Create a mock token
        TwitchToken token = new TwitchToken();
        token.setUserId(userId);
        token.setAccessToken("mock-access-token-" + UUID.randomUUID());
        token.setRefreshToken("mock-refresh-token-" + UUID.randomUUID());
        token.setScope("channel:read:predictions channel:manage:predictions user:read:email");
        token.setExpiresAt(LocalDateTime.now().plusHours(1));

        return tokenRepository.save(token);
    }

    public String getUserId(String accessToken) {
        // Extract mock user ID from the token or generate one
        if (accessToken.startsWith("mock-access-token-")) {
            return "mock-user-" + UUID.randomUUID().toString().substring(0, 8);
        }
        return "mock-user-default";
    }

    public Optional<TwitchToken> getTokenByUserId(String userId) {
        return tokenRepository.findById(userId);
    }

    public TwitchToken refreshToken(TwitchToken oldToken) {
        // Update expiration time
        oldToken.setExpiresAt(LocalDateTime.now().plusHours(1));

        // Generate new tokens
        oldToken.setAccessToken("mock-access-token-" + UUID.randomUUID());
        oldToken.setRefreshToken("mock-refresh-token-" + UUID.randomUUID());

        return tokenRepository.save(oldToken);
    }

    public String getValidAccessToken(String userId) {
        Optional<TwitchToken> tokenOpt = getTokenByUserId(userId);

        if (tokenOpt.isPresent()) {
            TwitchToken token = tokenOpt.get();

            // Refresh token if expired
            if (token.isExpired()) {
                token = refreshToken(token);
            }

            return token.getAccessToken();
        }

        // If no token exists, create a mock one
        TwitchToken newToken = new TwitchToken();
        newToken.setUserId(userId);
        newToken.setAccessToken("mock-access-token-" + UUID.randomUUID());
        newToken.setRefreshToken("mock-refresh-token-" + UUID.randomUUID());
        newToken.setScope("channel:read:predictions channel:manage:predictions user:read:email");
        newToken.setExpiresAt(LocalDateTime.now().plusHours(1));

        tokenRepository.save(newToken);
        return newToken.getAccessToken();
    }
}