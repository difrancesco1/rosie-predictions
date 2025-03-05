package com.example.backend.controller;

import com.example.backend.model.LeagueAccount;
import com.example.backend.service.LeagueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/league")
public class LeagueController {
    private final LeagueService leagueService;

    @Autowired
    public LeagueController(LeagueService leagueService) {
        this.leagueService = leagueService;
    }

    @GetMapping("/test")
    public ResponseEntity<String> testEndpoint() {
        return ResponseEntity.ok("League controller is working!");
    }

    // Connect a new account
    @PostMapping("/{userId}/connect")
    public ResponseEntity<?> connectAccount(
            @PathVariable String userId,
            @RequestBody Map<String, String> request) {

        String summonerName = request.get("summonerName");
        if (summonerName == null || summonerName.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Summoner name is required"));
        }

        try {
            LeagueAccount account = leagueService.connectAccount(userId, summonerName);
            return ResponseEntity.ok(account);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to connect account: " + e.getMessage()));
        }
    }

    // Get all accounts for a user
    @GetMapping("/{userId}/accounts")
    public ResponseEntity<List<LeagueAccount>> getAllAccounts(@PathVariable String userId) {
        List<LeagueAccount> accounts = leagueService.getAllAccountsByUserId(userId);
        return ResponseEntity.ok(accounts);
    }

    // Get active account for a user
    @GetMapping("/{userId}/active")
    public ResponseEntity<?> getActiveAccount(@PathVariable String userId) {
        Optional<LeagueAccount> account = leagueService.getActiveAccountByUserId(userId);
        if (account.isPresent()) {
            return ResponseEntity.ok(account.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // Set account as active
    @PatchMapping("/{userId}/accounts/{accountId}/activate")
    public ResponseEntity<?> setAccountActive(
            @PathVariable String userId,
            @PathVariable UUID accountId) {

        try {
            LeagueAccount account = leagueService.setAccountActive(userId, accountId);
            return ResponseEntity.ok(account);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Update account settings
    @PatchMapping("/{userId}/accounts/{accountId}/settings")
    public ResponseEntity<?> updateSettings(
            @PathVariable String userId,
            @PathVariable UUID accountId,
            @RequestBody Map<String, Boolean> settings) {

        Boolean autoCreate = settings.get("autoCreatePredictions");
        Boolean autoResolve = settings.get("autoResolvePredictions");

        if (autoCreate == null && autoResolve == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "No settings provided to update"));
        }

        try {
            LeagueAccount updatedAccount = leagueService.updateAccountSettings(
                    userId,
                    accountId,
                    autoCreate != null ? autoCreate : false,
                    autoResolve != null ? autoResolve : false);

            return ResponseEntity.ok(updatedAccount);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Delete an account
    @DeleteMapping("/{userId}/accounts/{accountId}")
    public ResponseEntity<?> disconnectAccount(
            @PathVariable String userId,
            @PathVariable UUID accountId) {

        try {
            leagueService.disconnectAccount(userId, accountId);
            return ResponseEntity.ok(Map.of("message", "Account successfully disconnected"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}