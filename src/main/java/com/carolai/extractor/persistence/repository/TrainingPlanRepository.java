package com.carolai.extractor.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.carolai.extractor.persistence.entity.TrainingPlanEntity;

public interface TrainingPlanRepository extends JpaRepository<TrainingPlanEntity, Long> {

        @Query("""
                SELECT tpe
                FROM TrainingPlanEntity tpe
                WHERE tpe.externalRef = :externalRef
        """)
        Optional<TrainingPlanEntity> findByExternalRef(String externalRef);
}