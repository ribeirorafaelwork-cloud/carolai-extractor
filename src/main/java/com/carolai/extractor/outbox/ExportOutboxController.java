package com.carolai.extractor.outbox;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.carolai.extractor.outbox.dto.AckRequest;
import com.carolai.extractor.outbox.dto.FailRequest;
import com.carolai.extractor.outbox.dto.OutboxItemResponse;
import com.carolai.extractor.outbox.dto.OutboxStatsResponse;
import com.carolai.extractor.outbox.dto.PopulateResult;
import com.carolai.extractor.persistence.entity.ExportOutboxEntity;
import com.carolai.extractor.persistence.entity.MigrationIdMapEntity;
import com.carolai.extractor.persistence.repository.ExportOutboxRepository;
import com.carolai.extractor.persistence.repository.MigrationIdMapRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/internal/exports")
public class ExportOutboxController {

    private final OutboxPopulationService populationService;
    private final ExportOutboxRepository outboxRepository;
    private final MigrationIdMapRepository migrationIdMapRepository;
    private final ObjectMapper jackson2Mapper = new ObjectMapper();

    public ExportOutboxController(
            OutboxPopulationService populationService,
            ExportOutboxRepository outboxRepository,
            MigrationIdMapRepository migrationIdMapRepository
    ) {
        this.populationService = populationService;
        this.outboxRepository = outboxRepository;
        this.migrationIdMapRepository = migrationIdMapRepository;
    }

    @PostMapping("/populate")
    public ResponseEntity<?> populate(@RequestParam(required = false) String type) {
        if (type != null && !type.isBlank()) {
            PopulateResult result = populationService.populate(type);
            return ResponseEntity.ok(result);
        }
        List<PopulateResult> results = populationService.populateAll();
        return ResponseEntity.ok(results);
    }

    @GetMapping("/pending")
    public ResponseEntity<List<OutboxItemResponse>> getPending(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "100") int limit
    ) {
        List<ExportOutboxEntity> items;
        if (type != null && !type.isBlank()) {
            items = outboxRepository.findByEntityTypeAndStatusOrderByCreatedAtAsc(
                    type.toUpperCase(), "PENDING", PageRequest.of(0, limit));
        } else {
            items = outboxRepository.findByStatusOrderByCreatedAtAsc(
                    "PENDING", PageRequest.of(0, limit));
        }

        List<OutboxItemResponse> response = items.stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    public ResponseEntity<List<OutboxItemResponse>> getAll(
            @RequestParam String type,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        List<ExportOutboxEntity> items = outboxRepository.findByEntityTypeOrderByCreatedAtAsc(
                type.toUpperCase(), PageRequest.of(offset / Math.max(limit, 1), limit));

        List<OutboxItemResponse> response = items.stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/ack")
    public ResponseEntity<Void> ack(@PathVariable Long id, @RequestBody AckRequest request) {
        ExportOutboxEntity entity = outboxRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Outbox item not found: " + id));

        entity.setStatus("ACKED");
        entity.setAckedAt(Instant.now());
        entity.setErrorMsg(null);
        outboxRepository.save(entity);

        // Update migration_id_map if platformId provided
        if (request.platformId() != null && !request.platformId().isBlank()) {
            migrationIdMapRepository.findByEntityTypeAndSourceKey(entity.getEntityType(), entity.getSourceKey())
                    .ifPresentOrElse(
                            existing -> {
                                existing.setPlatformId(request.platformId());
                                existing.setPayloadHash(entity.getPayloadHash());
                                migrationIdMapRepository.save(existing);
                            },
                            () -> {
                                MigrationIdMapEntity map = new MigrationIdMapEntity();
                                map.setEntityType(entity.getEntityType());
                                map.setSourceKey(entity.getSourceKey());
                                map.setPlatformId(request.platformId());
                                map.setPayloadHash(entity.getPayloadHash());
                                migrationIdMapRepository.save(map);
                            }
                    );
        }

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/fail")
    public ResponseEntity<Void> fail(@PathVariable Long id, @RequestBody FailRequest request) {
        ExportOutboxEntity entity = outboxRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Outbox item not found: " + id));

        entity.setStatus("FAILED");
        entity.setErrorMsg(request.error());
        outboxRepository.save(entity);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Map<String, Object>> reset(@RequestParam(required = false) String type) {
        int updated;
        if (type != null && !type.isBlank()) {
            updated = outboxRepository.resetToPendingByEntityType(type.toUpperCase());
        } else {
            updated = outboxRepository.resetAllToPending();
        }
        return ResponseEntity.ok(Map.of("resetCount", updated, "type", type != null ? type : "ALL"));
    }

    @GetMapping("/stats")
    public ResponseEntity<OutboxStatsResponse> stats() {
        List<Object[]> counts = outboxRepository.countByEntityTypeAndStatus();

        Map<String, Map<String, Long>> byTypeAndStatus = new HashMap<>();
        long totalPending = 0, totalAcked = 0, totalFailed = 0;

        for (Object[] row : counts) {
            String entityType = (String) row[0];
            String status = (String) row[1];
            Long count = (Long) row[2];

            byTypeAndStatus.computeIfAbsent(entityType, k -> new HashMap<>())
                    .put(status, count);

            switch (status) {
                case "PENDING" -> totalPending += count;
                case "ACKED" -> totalAcked += count;
                case "FAILED" -> totalFailed += count;
            }
        }

        return ResponseEntity.ok(new OutboxStatsResponse(byTypeAndStatus, totalPending, totalAcked, totalFailed));
    }

    @SuppressWarnings("unchecked")
    private OutboxItemResponse toResponse(ExportOutboxEntity entity) {
        // Convert Jackson 2.x JsonNode to Map for proper serialization by Jackson 3.x
        Map<String, Object> payloadMap = jackson2Mapper.convertValue(
                entity.getPayload(), Map.class);
        return new OutboxItemResponse(
                entity.getId(),
                entity.getEntityType(),
                entity.getSourceKey(),
                payloadMap,
                entity.getPayloadHash(),
                entity.getStatus(),
                entity.getCreatedAt().toString()
        );
    }
}
