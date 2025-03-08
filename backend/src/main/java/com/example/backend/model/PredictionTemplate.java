package com.example.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "prediction_templates")
public class PredictionTemplate {
    @Id
    @GeneratedValue
    private UUID id;

    private String userId;
    private String title;
    private String outcome1;
    private String outcome2;
    private int duration; // in seconds

    // Default constructor
    public PredictionTemplate() {
    }

    // Constructor with fields
    public PredictionTemplate(String userId, String title, String outcome1, String outcome2, int duration) {
        this.userId = userId;
        this.title = title;
        this.outcome1 = outcome1;
        this.outcome2 = outcome2;
        this.duration = duration;
    }

    // Getters and setters
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOutcome1() {
        return outcome1;
    }

    public void setOutcome1(String outcome1) {
        this.outcome1 = outcome1;
    }

    public String getOutcome2() {
        return outcome2;
    }

    public void setOutcome2(String outcome2) {
        this.outcome2 = outcome2;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }
}