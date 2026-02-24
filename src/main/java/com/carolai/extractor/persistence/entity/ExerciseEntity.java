package com.carolai.extractor.persistence.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import com.fasterxml.jackson.databind.JsonNode;

@Entity
@Table(name = "exercise")
public class ExerciseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String exerciseRef;
    private String exerciseName;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private String videoUrl;
    private String coverUrl;

    private Boolean freeExecution;
    private Boolean inHome;

    private Integer orderIndex;
    private Long createdAt;

    @ManyToOne
    @JoinColumn(name = "series_id")
    private SeriesEntity series;

    @Column(columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode executionMode;

    @Column(columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode muscleGroups;

    @Column(columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode equipments;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getExerciseRef() {
        return exerciseRef;
    }

    public void setExerciseRef(String exerciseRef) {
        this.exerciseRef = exerciseRef;
    }

    public String getExerciseName() {
        return exerciseName;
    }

    public void setExerciseName(String exerciseName) {
        this.exerciseName = exerciseName;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public Boolean getFreeExecution() {
        return freeExecution;
    }

    public void setFreeExecution(Boolean freeExecution) {
        this.freeExecution = freeExecution;
    }

    public Boolean getInHome() {
        return inHome;
    }

    public void setInHome(Boolean inHome) {
        this.inHome = inHome;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public SeriesEntity getSeries() {
        return series;
    }

    public void setSeries(SeriesEntity series) {
        this.series = series;
    }

    public JsonNode getExecutionMode() {
        return executionMode;
    }

    public void setExecutionMode(JsonNode executionMode) {
        this.executionMode = executionMode;
    }

    public JsonNode getMuscleGroups() {
        return muscleGroups;
    }

    public void setMuscleGroups(JsonNode muscleGroups) {
        this.muscleGroups = muscleGroups;
    }

    public JsonNode getEquipments() {
        return equipments;
    }

    public void setEquipments(JsonNode equipments) {
        this.equipments = equipments;
    }

    
}