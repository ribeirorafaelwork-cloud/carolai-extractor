package com.carolai.extractor.persistence.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "training")
public class TrainingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "external_ref", nullable = false)
    private String externalRef;

    private String personalId;
    private String personalName;
    private String coverUrl;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private String createdAt;

    @Column(name = "content_hash")
    private String contentHash;

    @OneToMany(mappedBy = "training", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GroupEntity> groups = new ArrayList<>();

    @OneToMany(mappedBy = "training",  cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TrainingPlanTrainingEntity> trainingPlanTraining = new ArrayList<>();

    public void addGroup(GroupEntity g) {
        groups.add(g);
        g.setTraining(this);
    }

    public void addTrainingPlanTraining(TrainingPlanTrainingEntity t) {
        trainingPlanTraining.add(t);
        t.setTraining(this);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExternalRef() {
        return externalRef;
    }

    public void setExternalRef(String externalRef) {
        this.externalRef = externalRef;
    }

    public String getPersonalId() {
        return personalId;
    }

    public void setPersonalId(String personalId) {
        this.personalId = personalId;
    }

    public String getPersonalName() {
        return personalName;
    }

    public void setPersonalName(String personalName) {
        this.personalName = personalName;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<GroupEntity> getGroups() {
        return groups;
    }

    public void setGroups(List<GroupEntity> groups) {
        this.groups = groups;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public List<TrainingPlanTrainingEntity> getTrainingPlanTraining() {
        return trainingPlanTraining;
    }

    public void setTrainingPlanTraining(List<TrainingPlanTrainingEntity> trainingPlanTraining) {
        this.trainingPlanTraining = trainingPlanTraining;
    }
}
