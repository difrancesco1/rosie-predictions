package com.example.backend.controller;

import com.example.backend.model.LeagueAccount;
import com.example.backend.repository.LeagueAccountRepository;
import com.example.backend.service.LeagueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/league")
public class LeagueController {
    private final LeagueService leagueService;
    private final LeagueAccountRepository leagueAccountRepository;

    @Autowired
    public LeagueController(LeagueService leagueService, LeagueAccountRepository leagueAccountRepository) {
        this.leagueService = leagueService;
        this.leagueAccountRepository = leagueAccountRepository;
        System.out.println("LeagueController initialized!");
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
            // First check if an account already exists and delete it
            try {
                leagueAccountRepository.deleteByUserId(userId);
            } catch (Exception e) {
                // Ignore errors, just continue to create a new account
                System.out.println("No existing account to delete: " + e.getMessage());
            }

            // Now create a new account
            LeagueAccount account = new LeagueAccount(userId, summonerName);
            account.setSummonerId("test-summoner-id-" + System.currentTimeMillis());
            account.setPuuid("test-puuid-" + System.currentTimeMillis());
            account.setRegion("na1");
            account.setAutoCreatePredictions(false);
            account.setAutoResolvePredictions(false);
            account.setActive(true);
            account = leagueAccountRepository.save(account);

            return ResponseEntity.ok(account);
        } catch (Exception e) {
            System.err.println("Error in connectAccount: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to connect account: " + e.getMessage()));
        }
    }

    // Get all accounts for a user
    @GetMapping("/{userId}/accounts")
    public ResponseEntity<List<LeagueAccount>> getAllAccounts(@PathVariable String userId) {
        try {
            List<LeagueAccount> accounts = leagueAccountRepository.findAllByUserId(userId);
            return ResponseEntity.ok(accounts);
        } catch (Exception e) {
            System.err.println("Error in getAllAccounts: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(Collections.emptyList()); // Return empty list on error
        }
    }

    // Get active account
    @GetMapping("/{userId}/active")
    public ResponseEntity<?> getActiveAccount(@PathVariable String userId) {
        try {
            Optional<LeagueAccount> account = leagueAccountRepository.findByUserIdAndIsActiveTrue(userId);
            if (account.isPresent()) {
                return ResponseEntity.ok(account.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            System.err.println("Error in getActiveAccount: " + e.getMessage());
            e.printStackTrace();
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
            System.err.println("Error in setAccountActive: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Update account settings
    @PatchMapping("/{userId}/accounts/{accountId}/settings")
    public ResponseEntity<?> updateSettings(
            @PathVariable String userId,
            @PathVariable UUID accountId,
            @RequestBody Map<String, Boolean> settings) {

        try {
            Boolean autoCreate = settings.get("autoCreatePredictions");
            Boolean autoResolve = settings.get("autoResolvePredictions");

            if (autoCreate == null && autoResolve == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "No settings provided to update"));
            }

            LeagueAccount account = leagueService.updateAccountSettings(
                    userId,
                    accountId,
                    autoCreate != null ? autoCreate : false,
                    autoResolve != null ? autoResolve : false);

            return ResponseEntity.ok(account);
        } catch (Exception e) {
            System.err.println("Error in updateSettings: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Disconnect an account
    @DeleteMapping("/{userId}/accounts/{accountId}")
    public ResponseEntity<?> disconnectAccount(
            @PathVariable String userId,
            @PathVariable UUID accountId) {

        try {
            leagueService.disconnectAccount(userId, accountId);
            return ResponseEntity.ok(Map.of("message", "Account successfully disconnected"));
        } catch (Exception e) {
            System.err.println("Error in disconnectAccount: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}