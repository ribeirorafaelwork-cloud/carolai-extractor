package com.carolai.extractor.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.carolai.extractor.persistence.entity.PhysicalAssessmentEntity;

@Repository
public interface PhysicalAssessmentRepository extends JpaRepository<PhysicalAssessmentEntity, Long> {

    Optional<PhysicalAssessmentEntity> findByExternalRef(String externalRef);

    Optional<PhysicalAssessmentEntity> findByCustomerIdAndDocumentKey(Long customerId, String documentKey);

}