package com.carolai.extractor.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.carolai.extractor.persistence.entity.GroupEntity;

public interface GroupRepository extends JpaRepository<GroupEntity, String> {
}