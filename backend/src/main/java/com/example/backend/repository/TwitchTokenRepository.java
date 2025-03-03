package com.example.backend.repository;

import com.example.backend.model.TwitchToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TwitchTokenRepository extends JpaRepository<TwitchToken, String> {
    // Additional query methods if needed
}