package com.carolai.extractor.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.carolai.extractor.persistence.entity.TrainingEntity;

public interface TrainingRepository extends JpaRepository<TrainingEntity, Long> {

    Optional<TrainingEntity> findByExternalRef(
            String externalRef
    );
}