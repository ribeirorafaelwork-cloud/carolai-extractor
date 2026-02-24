package com.carolai.extractor.persistence.entity;

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

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

     
}
