package com.carolai.extractor.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.carolai.extractor.persistence.entity.CustomerEntity;

public interface CustomerRepository
        extends JpaRepository<CustomerEntity, Long> {

    Optional<CustomerEntity> findByExternalRef(
            String externalRef
    );
}