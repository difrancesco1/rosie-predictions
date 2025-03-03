package com.example.backend.service;

import com.example.backend.config.TwitchConfig;
import com.example.backend.model.TwitchToken;
import com.example.backend.repository.TwitchTokenRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
public class TwitchAuthService {

    @Autowired
    private TwitchConfig twitchConfig;

    @Autowired
    private TwitchTokenRepository tokenRepository;

    @Autowired
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String getAuthorizationUrl() {
        String baseUrl = "https://id.twitch.tv/oauth2/authorize";
        String clientId = twitchConfig.getClientId();
        String redirectUri = twitchConfig.getRedirectUri();

        // Add required scopes for predictions
        String scopes = "channel:read:predictions channel:manage:predictions user:read:email";

        return String.format(
                "%s?client_id=%s&redirect_uri=%s&response_type=code&scope=%s",
                baseUrl, clientId, redirectUri, scopes);
    }

    public TwitchToken handleAuthCode(String code) {
        try {
            // Exchange authorization code for token
            String tokenUrl = "https://id.twitch.tv/oauth2/token";
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("client_id", twitchConfig.getClientId());
            params.add("client_secret", twitchConfig.getClientSecret());
            params.add("code", code);
            params.add("grant_type", "authorization_code");
            params.add("redirect_uri", twitchConfig.getRedirectUri());

            ResponseEntity<String> response = restTemplate.postForEntity(tokenUrl, params, String.class);

            // Parse the token response manually
            JsonNode tokenNode = objectMapper.readTree(response.getBody());

            // Get user ID with token
            String accessToken = tokenNode.get("access_token").asText();
            String userId = getUserId(accessToken);

            // Save token to database
            TwitchToken token = new TwitchToken();
            token.setUserId(userId);
            token.setAccessToken(accessToken);
            token.setRefreshToken(tokenNode.get("refresh_token").asText());
            token.setScope(tokenNode.get("scope").asText());

            int expiresIn = tokenNode.get("expires_in").asInt();
            token.setExpiresAt(LocalDateTime.now().plusSeconds(expiresIn));

            return tokenRepository.save(token);
        } catch (Exception e) {
            System.err.println("Error in handleAuthCode: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to authenticate with Twitch", e);
        }
    }

    public String getUserId(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("Client-Id", twitchConfig.getClientId());

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            // Get the response as a String
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://api.twitch.tv/helix/users",
                    HttpMethod.GET,
                    entity,
                    String.class);

            // Parse the JSON response manually
            String responseBody = response.getBody();
            System.out.println("Twitch API response: " + responseBody);

            // Parse the response using Jackson
            JsonNode rootNode = objectMapper.readTree(responseBody);

            // Get the data array
            JsonNode dataNode = rootNode.get("data");
            if (dataNode != null && dataNode.isArray() && dataNode.size() > 0) {
                // Get the first user object
                JsonNode userNode = dataNode.get(0);

                // Get the id field
                JsonNode idNode = userNode.get("id");
                if (idNode != null) {
                    return idNode.asText();
                }
            }

            throw new RuntimeException("User ID not found in Twitch API response");
        } catch (Exception e) {
            System.err.println("Error parsing Twitch API response: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to get user ID from Twitch", e);
        }
    }

    public Optional<TwitchToken> getTokenByUserId(String userId) {
        return tokenRepository.findById(userId);
    }

    public TwitchToken refreshToken(TwitchToken oldToken) {
        try {
            String tokenUrl = "https://id.twitch.tv/oauth2/token";
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("client_id", twitchConfig.getClientId());
            params.add("client_secret", twitchConfig.getClientSecret());
            params.add("refresh_token", oldToken.getRefreshToken());
            params.add("grant_type", "refresh_token");

            ResponseEntity<String> response = restTemplate.postForEntity(tokenUrl, params, String.class);

            // Parse the token response manually
            JsonNode tokenNode = objectMapper.readTree(response.getBody());

            // Update token data
            oldToken.setAccessToken(tokenNode.get("access_token").asText());
            oldToken.setRefreshToken(tokenNode.get("refresh_token").asText());

            int expiresIn = tokenNode.get("expires_in").asInt();
            oldToken.setExpiresAt(LocalDateTime.now().plusSeconds(expiresIn));

            return tokenRepository.save(oldToken);
        } catch (Exception e) {
            System.err.println("Error refreshing token: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to refresh token", e);
        }
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

        return null;
    }
}