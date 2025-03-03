package com.example.backend.controller;

import com.example.backend.model.TwitchToken;
import com.example.backend.service.TwitchAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth/twitch")
public class TwitchAuthController {
    @Autowired
    private TwitchAuthService twitchAuthService;

    @GetMapping("/url")
    public ResponseEntity<Map<String, String>> getAuthUrl() {
        Map<String, String> response = new HashMap<>();
        response.put("url", twitchAuthService.getAuthorizationUrl());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/callback")
    public ResponseEntity<?> handleCallback(@RequestParam String code) {
        try {
            System.out.println("Received POST callback code: " + code);

            TwitchToken token = twitchAuthService.handleAuthCode(code);

            Map<String, Object> response = new HashMap<>();
            response.put("userId", token.getUserId());
            response.put("success", true);

            System.out.println("Authentication successful for user: " + token.getUserId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error in handleCallback: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/callback")
    public ResponseEntity<?> handleCallbackGet(@RequestParam String code) {
        try {
            System.out.println("Received callback code: " + code);

            TwitchToken token = twitchAuthService.handleAuthCode(code);

            Map<String, Object> response = new HashMap<>();
            response.put("userId", token.getUserId());
            response.put("success", true);

            System.out.println("Authentication successful for user: " + token.getUserId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error in handleCallbackGet: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/status/{userId}")
    public ResponseEntity<?> checkAuthStatus(@PathVariable String userId) {
        boolean isAuthenticated = twitchAuthService.getTokenByUserId(userId).isPresent();

        Map<String, Object> response = new HashMap<>();
        response.put("authenticated", isAuthenticated);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout/{userId}")
    public ResponseEntity<?> logout(@PathVariable String userId) {
        twitchAuthService.getTokenByUserId(userId).ifPresent(token -> {
            // Implement token revocation logic if needed
        });

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);

        return ResponseEntity.ok(response);
    }
}