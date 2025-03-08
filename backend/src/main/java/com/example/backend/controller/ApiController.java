package com.example.backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;

@RestController
@CrossOrigin(origins = "*") // For development only
public class ApiController {

    @Autowired
    private Environment environment;

    @Value("${app.mock.enabled:false}")
    private boolean mockEnabled;

    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "running");
        response.put("timestamp", System.currentTimeMillis());

        // Check if we're running in mock mode
        boolean isMockMode = mockEnabled ||
                Arrays.asList(environment.getActiveProfiles()).contains("mock");

        response.put("mockMode", isMockMode);

        return response;
    }
}