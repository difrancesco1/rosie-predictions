package com.example.backend.repository;

import com.example.backend.model.PredictionSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PredictionSessionRepository extends JpaRepository<PredictionSession, String> {
    List<PredictionSession> findByBroadcasterIdOrderByStartedAtDesc(String broadcasterId);

    Page<PredictionSession> findByBroadcasterIdAndStartedAtBetweenOrderByStartedAtDesc(
            String broadcasterId, LocalDateTime start, LocalDateTime end, Pageable pageable);

    List<PredictionSession> findByBroadcasterIdAndStatusOrderByStartedAtDesc(String broadcasterId, String status);
}