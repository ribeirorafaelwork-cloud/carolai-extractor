package com.carolai.extractor.persistence.entity;

import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "physical_assessment",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_physical_assessment_customer_document",
            columnNames = {"customer_id", "document_key"}
        )
    },
    indexes = {
        @Index(
            name = "idx_physical_assessment_customer",
            columnList = "customer_id"
        )
    }
)
public class PhysicalAssessmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "external_ref", nullable = false, length = 255)
    private String externalRef;

    @Column(name = "document_key", nullable = false, length = 128)
    private String documentKey;

    @Column(name = "assessment_created_at")
    private Long assessmentCreatedAt;

    @Column(name = "assessment_answered_at")
    private Long assessmentAnsweredAt;

    @Column(name = "created_at", length = 64)
    private String createdAt;

    @Column(name = "last_modified_at", length = 64)
    private String lastModifiedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_json", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> rawJson;

    @Column(name = "content_hash", length = 255)
    private String contentHash;

    public Long getId() {
        return id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getExternalRef() {
        return externalRef;
    }

    public void setExternalRef(String externalRef) {
        this.externalRef = externalRef;
    }

    public String getDocumentKey() {
        return documentKey;
    }

    public void setDocumentKey(String documentKey) {
        this.documentKey = documentKey;
    }

    public Long getAssessmentCreatedAt() {
        return assessmentCreatedAt;
    }

    public void setAssessmentCreatedAt(Long assessmentCreatedAt) {
        this.assessmentCreatedAt = assessmentCreatedAt;
    }

    public Long getAssessmentAnsweredAt() {
        return assessmentAnsweredAt;
    }

    public void setAssessmentAnsweredAt(Long assessmentAnsweredAt) {
        this.assessmentAnsweredAt = assessmentAnsweredAt;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getLastModifiedAt() {
        return lastModifiedAt;
    }

    public void setLastModifiedAt(String lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
    }

    public Map<String, Object> getRawJson() {
        return rawJson;
    }

    public void setRawJson(Map<String, Object> rawJson) {
        this.rawJson = rawJson;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }
}