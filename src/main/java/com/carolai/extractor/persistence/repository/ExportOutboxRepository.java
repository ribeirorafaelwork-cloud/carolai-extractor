package com.carolai.extractor.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.carolai.extractor.persistence.entity.ExportOutboxEntity;

public interface ExportOutboxRepository extends JpaRepository<ExportOutboxEntity, Long> {

    List<ExportOutboxEntity> findByStatusOrderByCreatedAtAsc(String status, Pageable pageable);

    List<ExportOutboxEntity> findByEntityTypeAndStatusOrderByCreatedAtAsc(
            String entityType, String status, Pageable pageable);

    List<ExportOutboxEntity> findByEntityTypeOrderByCreatedAtAsc(String entityType, Pageable pageable);

    Optional<ExportOutboxEntity> findByEntityTypeAndSourceKey(String entityType, String sourceKey);

    @Query("SELECT e.entityType, e.status, COUNT(e) FROM ExportOutboxEntity e GROUP BY e.entityType, e.status")
    List<Object[]> countByEntityTypeAndStatus();

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE ExportOutboxEntity e SET e.status = 'PENDING', e.ackedAt = null, e.errorMsg = null WHERE e.status IN ('ACKED', 'FAILED')")
    int resetAllToPending();

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE ExportOutboxEntity e SET e.status = 'PENDING', e.ackedAt = null, e.errorMsg = null WHERE e.entityType = :entityType AND e.status IN ('ACKED', 'FAILED')")
    int resetToPendingByEntityType(String entityType);
}
