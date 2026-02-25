package com.carolai.extractor.migration;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import com.carolai.extractor.migration.dto.CreateStudentPayload;
import com.carolai.extractor.migration.dto.MigrationOptions;
import com.carolai.extractor.migration.dto.PlatformStudentResponse;
import com.carolai.extractor.migration.dto.SeedExercisesResponse;
import com.carolai.extractor.migration.mapper.MigrationHashUtil;
import com.carolai.extractor.migration.mapper.StudentMigrationMapper;
import com.carolai.extractor.persistence.entity.CustomerEntity;
import com.carolai.extractor.persistence.entity.MigrationIdMapEntity;
import com.carolai.extractor.persistence.entity.MigrationRunEntity;
import com.carolai.extractor.persistence.repository.CustomerRepository;
import com.carolai.extractor.persistence.repository.MigrationIdMapRepository;
import com.carolai.extractor.persistence.repository.MigrationRunRepository;

@Component
public class MigrationRunner {

    private static final Logger log = LogManager.getLogger(MigrationRunner.class);
    private static final String ENTITY_TYPE_STUDENT = "STUDENT";

    private final CustomerRepository customerRepository;
    private final MigrationIdMapRepository idMapRepository;
    private final MigrationRunRepository runRepository;
    private final StudentMigrationMapper studentMapper;
    private final PlatformApiClient platformClient;

    public MigrationRunner(CustomerRepository customerRepository,
                           MigrationIdMapRepository idMapRepository,
                           MigrationRunRepository runRepository,
                           StudentMigrationMapper studentMapper,
                           PlatformApiClient platformClient) {
        this.customerRepository = customerRepository;
        this.idMapRepository = idMapRepository;
        this.runRepository = runRepository;
        this.studentMapper = studentMapper;
        this.platformClient = platformClient;
    }

    public MigrationRunEntity runStudentMigration(MigrationOptions options) {
        log.info("Starting STUDENT migration — dryRun={}, limit={}, resume={}",
                options.dryRun(), options.limit(), options.resume());

        MigrationRunEntity run = createRun("STUDENT", options.dryRun());

        List<CustomerEntity> customers = customerRepository.findAll();
        int total = customers.size();
        int migrated = 0;
        int skipped = 0;
        int failed = 0;
        StringBuilder errorLog = new StringBuilder();

        if (!options.dryRun()) {
            platformClient.authenticate();
        }

        for (CustomerEntity customer : customers) {
            if (options.limit() > 0 && (migrated + skipped + failed) >= options.limit()) {
                break;
            }

            String sourceKey = studentMapper.sourceKey(customer);
            if (sourceKey == null || sourceKey.isBlank()) {
                log.warn("Customer id={} has no externalRef, skipping", customer.getId());
                skipped++;
                continue;
            }

            var existing = idMapRepository.findByEntityTypeAndSourceKey(ENTITY_TYPE_STUDENT, sourceKey);

            CreateStudentPayload payload = studentMapper.toPayload(customer);
            String payloadHash = MigrationHashUtil.sha256(payload);

            if (existing.isPresent()) {
                if (options.resume() && payloadHash.equals(existing.get().getPayloadHash())) {
                    log.debug("SKIP (unchanged) sourceKey={}", sourceKey);
                    skipped++;
                    continue;
                }
                if (options.resume()) {
                    log.debug("SKIP (already migrated) sourceKey={}", sourceKey);
                    skipped++;
                    continue;
                }
            }

            if (options.dryRun()) {
                log.info("[DRY-RUN] Would migrate customer sourceKey={} name={}", sourceKey, payload.fullName());
                migrated++;
                continue;
            }

            try {
                PlatformStudentResponse response = platformClient.createStudent(payload);
                log.info("Migrated student sourceKey={} -> platformId={}", sourceKey, response.id());

                saveIdMap(ENTITY_TYPE_STUDENT, sourceKey, response.id(), payloadHash);
                migrated++;
            } catch (Exception e) {
                log.error("Failed to migrate customer sourceKey={}: {}", sourceKey, e.getMessage());
                failed++;
                errorLog.append("sourceKey=").append(sourceKey).append(": ").append(e.getMessage()).append("\n");
            }
        }

        return finalizeRun(run, total, migrated, skipped, failed, errorLog.toString());
    }

    public MigrationRunEntity runExerciseSeed(MigrationOptions options) {
        log.info("Starting EXERCISE seed — dryRun={}", options.dryRun());

        MigrationRunEntity run = createRun("EXERCISE", options.dryRun());

        if (options.dryRun()) {
            log.info("[DRY-RUN] Would call POST /exercises/seed");
            return finalizeRun(run, 0, 0, 0, 0, "");
        }

        try {
            platformClient.authenticate();
            SeedExercisesResponse response = platformClient.seedExercises();
            log.info("Exercise seed completed: {} added", response.added());
            return finalizeRun(run, response.added(), response.added(), 0, 0, "");
        } catch (Exception e) {
            log.error("Exercise seed failed: {}", e.getMessage());
            return finalizeRun(run, 0, 0, 0, 1, e.getMessage());
        }
    }

    private MigrationRunEntity createRun(String runType, boolean dryRun) {
        MigrationRunEntity run = new MigrationRunEntity();
        run.setRunType(runType);
        run.setStatus("RUNNING");
        run.setDryRun(dryRun);
        return runRepository.save(run);
    }

    private MigrationRunEntity finalizeRun(MigrationRunEntity run, int total, int migrated, int skipped, int failed, String errors) {
        run.setTotal(total);
        run.setMigrated(migrated);
        run.setSkipped(skipped);
        run.setFailed(failed);
        run.setStatus(failed > 0 ? "COMPLETED_WITH_ERRORS" : "COMPLETED");
        run.setFinishedAt(java.time.Instant.now());
        if (errors != null && !errors.isBlank()) {
            run.setErrorLog(errors);
        }
        return runRepository.save(run);
    }

    private void saveIdMap(String entityType, String sourceKey, String platformId, String payloadHash) {
        var existing = idMapRepository.findByEntityTypeAndSourceKey(entityType, sourceKey);
        MigrationIdMapEntity entity;
        if (existing.isPresent()) {
            entity = existing.get();
            entity.setPlatformId(platformId);
            entity.setPayloadHash(payloadHash);
        } else {
            entity = new MigrationIdMapEntity();
            entity.setEntityType(entityType);
            entity.setSourceKey(sourceKey);
            entity.setPlatformId(platformId);
            entity.setPayloadHash(payloadHash);
        }
        idMapRepository.save(entity);
    }
}
