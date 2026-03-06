package com.carolai.extractor.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

        @Modifying
        @Query(value = """
                UPDATE training_plan_training tpt
                SET student_exercises_json = CAST(:exercisesJson AS jsonb),
                    student_training_name = :studentName
                FROM training t, training_plan tp
                WHERE tpt.training_id = t.id
                AND tpt.training_plan_id = tp.id
                AND t.external_ref = :trainingRef
                AND tp.external_ref = :planRef
        """, nativeQuery = true)
        int updateStudentExercises(@Param("trainingRef") String trainingRef,
                                   @Param("planRef") String planRef,
                                   @Param("exercisesJson") String exercisesJson,
                                   @Param("studentName") String studentName);

}