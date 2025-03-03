package com.example.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "predictions")
public class Prediction {

    @Id
    private String id;

    private String title;

    private String broadcasterId;

    private String sessionId;

    @Column(length = 1000)
    private String description;

    private LocalDateTime createdAt;

    private LocalDateTime endedAt;

    private LocalDateTime lockedAt;

    private String status; // ACTIVE, RESOLVED, CANCELED, LOCKED

    private String winningOutcomeId;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "prediction_id")
    private List<PredictionOutcome> outcomes = new ArrayList<>();

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBroadcasterId() {
        return broadcasterId;
    }

    public void setBroadcasterId(String broadcasterId) {
        this.broadcasterId = broadcasterId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }

    public LocalDateTime getLockedAt() {
        return lockedAt;
    }

    public void setLockedAt(LocalDateTime lockedAt) {
        this.lockedAt = lockedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getWinningOutcomeId() {
        return winningOutcomeId;
    }

    public void setWinningOutcomeId(String winningOutcomeId) {
        this.winningOutcomeId = winningOutcomeId;
    }

    public List<PredictionOutcome> getOutcomes() {
        return outcomes;
    }

    public void setOutcomes(List<PredictionOutcome> outcomes) {
        this.outcomes = outcomes;
    }
}