package com.example.backend.repository;

import com.example.backend.model.PredictionTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PredictionTemplateRepository extends JpaRepository<PredictionTemplate, UUID> {
    List<PredictionTemplate> findByUserId(String userId);
}