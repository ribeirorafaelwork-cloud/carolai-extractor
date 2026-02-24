package com.carolai.extractor.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.carolai.extractor.persistence.entity.ExerciseEntity;

public interface ExerciseRepository extends JpaRepository<ExerciseEntity, String> {
}