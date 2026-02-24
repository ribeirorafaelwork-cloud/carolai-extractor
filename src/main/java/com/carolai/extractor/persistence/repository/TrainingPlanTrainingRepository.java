package com.carolai.extractor.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.carolai.extractor.persistence.entity.TrainingPlanTrainingEntity;

public interface TrainingPlanTrainingRepository
        extends JpaRepository<TrainingPlanTrainingEntity, Long> {

        @Query("""
                SELECT tpt
                FROM TrainingPlanTrainingEntity tpt
                JOIN tpt.training t
                JOIN tpt.trainingPlan tp
                WHERE t.externalRef = :trainingRef
                AND tp.externalRef = :planRef
        """)
        Optional<TrainingPlanTrainingEntity> findByRefs(@Param("trainingRef") String trainingRef, 
                                                        @Param("planRef") String planRef);

}