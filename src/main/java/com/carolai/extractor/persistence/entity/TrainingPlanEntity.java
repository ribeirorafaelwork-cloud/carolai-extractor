package com.carolai.extractor.persistence.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;


@Entity
@Table(
    name = "training_plan",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_training_plan_customer_external",
        columnNames = {"customer_id", "external_ref"}
    )
)
public class TrainingPlanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerEntity customer;

    @Column(name = "external_ref", nullable = false)
    private String externalRef;

    private String startAt;
    private String endAt;

    private Boolean active;
    private String name;
    private String executionWeek;
    private String plannedSessions;

    @Column(name = "content_hash")
    private String contentHash;

    @OneToMany(
        mappedBy = "trainingPlan",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<TrainingPlanTrainingEntity> trainingPlanTraining = new ArrayList<>();

    public void addTrainingPlanTraining(TrainingPlanTrainingEntity ct) {
        trainingPlanTraining.add(ct);
        ct.setTrainingPlan(this);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CustomerEntity getCustomer() {
        return customer;
    }

    public void setCustomer(CustomerEntity customer) {
        this.customer = customer;
    }

    public String getExternalRef() {
        return externalRef;
    }

    public void setExternalRef(String externalRef) {
        this.externalRef = externalRef;
    }

    public String getStartAt() {
        return startAt;
    }

    public void setStartAt(String startAt) {
        this.startAt = startAt;
    }

    public String getEndAt() {
        return endAt;
    }

    public void setEndAt(String endAt) {
        this.endAt = endAt;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getExecutionWeek() {
        return executionWeek;
    }

    public void setExecutionWeek(String executionWeek) {
        this.executionWeek = executionWeek;
    }

    public String getPlannedSessions() {
        return plannedSessions;
    }

    public void setPlannedSessions(String plannedSessions) {
        this.plannedSessions = plannedSessions;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<TrainingPlanTrainingEntity> getTrainingPlanTraining() {
        return trainingPlanTraining;
    }

    public void setTrainingPlanTraining(List<TrainingPlanTrainingEntity> trainingPlanTraining) {
        this.trainingPlanTraining = trainingPlanTraining;
    }
    
}
