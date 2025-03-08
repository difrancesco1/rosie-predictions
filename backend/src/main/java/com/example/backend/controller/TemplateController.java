package com.example.backend.controller;

import com.example.backend.model.PredictionTemplate;
import com.example.backend.model.LeagueAccount;
import com.example.backend.repository.PredictionTemplateRepository;
import com.example.backend.repository.LeagueAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/templates")
public class TemplateController {

    @Autowired
    private PredictionTemplateRepository templateRepository;

    @Autowired
    private LeagueAccountRepository leagueAccountRepository;

    // Get all templates for a user
    @GetMapping("/{userId}")
    public ResponseEntity<List<PredictionTemplate>> getTemplates(@PathVariable String userId) {
        List<PredictionTemplate> templates = templateRepository.findByUserId(userId);
        return ResponseEntity.ok(templates);
    }

    // Create a new template
    @PostMapping("/{userId}")
    public ResponseEntity<PredictionTemplate> createTemplate(
            @PathVariable String userId,
            @RequestBody PredictionTemplate template) {

        template.setUserId(userId);
        PredictionTemplate saved = templateRepository.save(template);
        return ResponseEntity.ok(saved);
    }

    // Assign a template to a League account
    @PostMapping("/{userId}/assign/{accountId}/{templateId}")
    public ResponseEntity<?> assignTemplate(
            @PathVariable String userId,
            @PathVariable UUID accountId,
            @PathVariable UUID templateId) {

        try {
            // Verify template exists and belongs to user
            Optional<PredictionTemplate> templateOpt = templateRepository.findById(templateId);
            if (templateOpt.isEmpty() || !templateOpt.get().getUserId().equals(userId)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Template not found"));
            }

            // Get the account
            Optional<LeagueAccount> accountOpt = leagueAccountRepository.findById(accountId);
            if (accountOpt.isEmpty() || !accountOpt.get().getUserId().equals(userId)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Account not found"));
            }

            // Assign template to account
            LeagueAccount account = accountOpt.get();
            account.setActiveTemplateId(templateId);
            leagueAccountRepository.save(account);

            return ResponseEntity.ok(Map.of(
                    "message", "Template assigned successfully",
                    "account", account));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Clear template from account
    @DeleteMapping("/{userId}/assign/{accountId}")
    public ResponseEntity<?> clearTemplate(
            @PathVariable String userId,
            @PathVariable UUID accountId) {

        try {
            // Get the account
            Optional<LeagueAccount> accountOpt = leagueAccountRepository.findById(accountId);
            if (accountOpt.isEmpty() || !accountOpt.get().getUserId().equals(userId)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Account not found"));
            }

            // Clear template from account
            LeagueAccount account = accountOpt.get();
            account.setActiveTemplateId(null);
            leagueAccountRepository.save(account);

            return ResponseEntity.ok(Map.of(
                    "message", "Template removed from account",
                    "account", account));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}