package com.example.backend.repository;

import com.example.backend.model.LeagueAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeagueAccountRepository extends JpaRepository<LeagueAccount, UUID> {
    // Find all accounts for a user
    List<LeagueAccount> findByUserId(String userId);

    // Find by summoner name (for checking duplicates)
    Optional<LeagueAccount> findBySummonerName(String summonerName);

    // Find active account for a user
    Optional<LeagueAccount> findByUserIdAndIsActiveTrue(String userId);

    // Find accounts with auto features enabled (active accounts only)
    List<LeagueAccount> findByAutoCreatePredictionsTrueAndIsActiveTrue();

    List<LeagueAccount> findByAutoResolvePredictionsTrueAndIsActiveTrue();
}