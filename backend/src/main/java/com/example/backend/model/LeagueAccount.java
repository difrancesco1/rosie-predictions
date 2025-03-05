package com.example.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "league_accounts")
public class LeagueAccount {
    @Id
    @GeneratedValue
    private UUID id;

    private String userId;
    private String summonerName;
    private String summonerId;
    private String puuid;
    private boolean autoCreatePredictions;
    private boolean autoResolvePredictions;
    private LocalDateTime lastGameCheckTime;
    private String region;
    private boolean isActive;

    public LeagueAccount() {
    }

    public LeagueAccount(String userId, String summonerName) {
        this.userId = userId;
        this.summonerName = summonerName;
        this.autoCreatePredictions = false;
        this.autoResolvePredictions = false;
        this.lastGameCheckTime = LocalDateTime.now();
        this.isActive = true; // New accounts are active by default
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSummonerName() {
        return summonerName;
    }

    public void setSummonerName(String summonerName) {
        this.summonerName = summonerName;
    }

    public String getSummonerId() {
        return summonerId;
    }

    public void setSummonerId(String summonerId) {
        this.summonerId = summonerId;
    }

    public String getPuuid() {
        return puuid;
    }

    public void setPuuid(String puuid) {
        this.puuid = puuid;
    }

    public boolean isAutoCreatePredictions() {
        return autoCreatePredictions;
    }

    public void setAutoCreatePredictions(boolean autoCreatePredictions) {
        this.autoCreatePredictions = autoCreatePredictions;
    }

    public boolean isAutoResolvePredictions() {
        return autoResolvePredictions;
    }

    public void setAutoResolvePredictions(boolean autoResolvePredictions) {
        this.autoResolvePredictions = autoResolvePredictions;
    }

    public LocalDateTime getLastGameCheckTime() {
        return lastGameCheckTime;
    }

    public void setLastGameCheckTime(LocalDateTime lastGameCheckTime) {
        this.lastGameCheckTime = lastGameCheckTime;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}