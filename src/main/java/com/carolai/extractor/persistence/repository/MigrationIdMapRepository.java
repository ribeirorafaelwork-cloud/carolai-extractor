package com.carolai.extractor.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.carolai.extractor.persistence.entity.MigrationIdMapEntity;

public interface MigrationIdMapRepository extends JpaRepository<MigrationIdMapEntity, Long> {

    Optional<MigrationIdMapEntity> findByEntityTypeAndSourceKey(String entityType, String sourceKey);

    boolean existsByEntityTypeAndSourceKey(String entityType, String sourceKey);
}
