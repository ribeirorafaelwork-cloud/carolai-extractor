package com.carolai.extractor.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.carolai.extractor.persistence.entity.SeriesEntity;

public interface SerieRepository extends JpaRepository<SeriesEntity, String> {
}