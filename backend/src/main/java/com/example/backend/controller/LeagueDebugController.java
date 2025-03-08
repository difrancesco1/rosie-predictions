package com.example.backend.controller;

import com.example.backend.model.LeagueAccount;
import com.example.backend.service.LeagueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Debug controller for testing League integration features
 */
@RestController
@RequestMapping("/debug/league")
public class LeagueDebugController {

    @Autowired
    private LeagueService leagueService;

    /**
     * Manually trigger game check for testing
     */
    @PostMapping("/check-games")
    public ResponseEntity<Map<String, Object>> triggerGameCheck() {
        try {
            // Call the scheduled method directly using the correct method name
            leagueService.checkGameStatus();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Game check triggered successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());

            return ResponseEntity.badRequest().body(error);
        }
    }
}