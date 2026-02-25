package com.carolai.extractor.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.carolai.extractor.persistence.entity.MigrationRunEntity;

public interface MigrationRunRepository extends JpaRepository<MigrationRunEntity, Long> {

    List<MigrationRunEntity> findAllByOrderByStartedAtDesc();
}
