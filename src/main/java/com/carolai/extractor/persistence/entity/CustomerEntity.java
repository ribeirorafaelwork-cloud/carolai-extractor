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
import jakarta.persistence.UniqueConstraint;


@Entity
@Table(
    name = "customer",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_customer_external_tenant",
        columnNames = {"external_ref", "tenant_id"}
    )
)
public class CustomerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_ref", nullable = false)
    private String externalRef;

    private String name;
    private String email;
    private String doc;

    @Column(length = 1)
    private String gender;

    private String birthDate;

    private String phone;

    private String createdAt;
    
    private String updatedAt;

    @Column(name = "content_hash")
    private String contentHash;

    @OneToMany(
        mappedBy = "customer",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<CustomerObjectiveEntity> objectives = new ArrayList<>();

    @OneToMany(
        mappedBy = "customer",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<TrainingPlanEntity> trainingPlans = new ArrayList<>();
    
    public void addObjective(CustomerObjectiveEntity obj) {
        objectives.add(obj);
        obj.setCustomer(this);
    }

    public void addTrainingPlan(TrainingPlanEntity ct) {
        trainingPlans.add(ct);
        ct.setCustomer(this);
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getExternalRef() {
        return externalRef;
    }
    public void setExternalRef(String externalRef) {
        this.externalRef = externalRef;
    }
    
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getGender() {
        return gender;
    }
    public void setGender(String gender) {
        this.gender = gender;
    }
    
    public String getBirthDate() {
        return birthDate;
    }
    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }
    public String getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    public String getUpdatedAt() {
        return updatedAt;
    }
    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
    public String getContentHash() {
        return contentHash;
    }
    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public List<CustomerObjectiveEntity> getObjectives() {
        return objectives;
    }
    public void setObjectives(List<CustomerObjectiveEntity> objectives) {
        this.objectives = objectives;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getDoc() {
        return doc;
    }

    public void setDoc(String doc) {
        this.doc = doc;
    }

    public List<TrainingPlanEntity> getTrainingPlans() {
        return trainingPlans;
    }

    public void setTrainingPlans(List<TrainingPlanEntity> trainingPlans) {
        this.trainingPlans = trainingPlans;
    }
    
}