package com.example.backend.repository;

import com.example.backend.model.Prediction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PredictionRepository extends JpaRepository<Prediction, String> {
        List<Prediction> findByBroadcasterIdOrderByCreatedAtDesc(String broadcasterId);

        List<Prediction> findByBroadcasterIdAndSessionIdOrderByCreatedAtDesc(String broadcasterId, String sessionId);

        Page<Prediction> findByBroadcasterIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                        String broadcasterId, LocalDateTime start, LocalDateTime end, Pageable pageable);

        Page<Prediction> findByBroadcasterIdAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(
                        String broadcasterId, String titleQuery, Pageable pageable);

        List<Prediction> findByBroadcasterIdAndStatus(String broadcasterId, String status);

        @Query("SELECT p FROM Prediction p WHERE p.broadcasterId = :userId " +
                        "AND p.createdAt BETWEEN :startDate AND :endDate " +
                        "ORDER BY p.createdAt DESC")
        List<Prediction> findPredictionsInDateRange(
                        @Param("userId") String userId,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);
}