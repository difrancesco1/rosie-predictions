package com.example.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LeagueConfig {
    @Value("${league.api-key}")
    private String apiKey;

    @Value("${league.americas-base-url:https://americas.api.riotgames.com}")
    private String americasBaseUrl;

    @Value("${league.na-base-url:https://na1.api.riotgames.com}")
    private String naBaseUrl;

    @Value("${league.polling-interval:60}")
    private int pollingIntervalSeconds;

    public String getApiKey() {
        return apiKey;
    }

    public String getAmericasBaseUrl() {
        return americasBaseUrl;
    }

    public String getNaBaseUrl() {
        return naBaseUrl;
    }

    public int getPollingIntervalSeconds() {
        return pollingIntervalSeconds;
    }
}