package com.carolai.extractor.persistence.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;


@Entity
@Table(
    name = "training_plan_training",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_training_plan_training_unique",
        columnNames = {"training_plan_id", "training_id"}
    )
)
public class TrainingPlanTrainingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "content_hash")
    private String contentHash;

    @ManyToOne
    @JoinColumn(name = "training_plan_id")
    private TrainingPlanEntity trainingPlan;

    @ManyToOne
    @JoinColumn(name = "training_id")
    private TrainingEntity training;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TrainingPlanEntity getTrainingPlan() {
        return trainingPlan;
    }

    public void setTrainingPlan(TrainingPlanEntity trainingPlan) {
        this.trainingPlan = trainingPlan;
    }

    public TrainingEntity getTraining() {
        return training;
    }

    public void setTraining(TrainingEntity training) {
        this.training = training;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    @Column(name = "student_exercises_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode studentExercisesJson;

    @Column(name = "student_training_name")
    private String studentTrainingName;

    public JsonNode getStudentExercisesJson() {
        return studentExercisesJson;
    }

    public void setStudentExercisesJson(JsonNode studentExercisesJson) {
        this.studentExercisesJson = studentExercisesJson;
    }

    public String getStudentTrainingName() {
        return studentTrainingName;
    }

    public void setStudentTrainingName(String studentTrainingName) {
        this.studentTrainingName = studentTrainingName;
    }
}
