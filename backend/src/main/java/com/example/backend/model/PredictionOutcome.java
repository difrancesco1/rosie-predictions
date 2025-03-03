package com.example.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "prediction_outcomes")
public class PredictionOutcome {

    @Id
    private String id;

    private String title;

    private String color; // BLUE, PINK

    private int users;

    private int channelPoints;

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

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public int getUsers() {
        return users;
    }

    public void setUsers(int users) {
        this.users = users;
    }

    public int getChannelPoints() {
        return channelPoints;
    }

    public void setChannelPoints(int channelPoints) {
        this.channelPoints = channelPoints;
    }
}