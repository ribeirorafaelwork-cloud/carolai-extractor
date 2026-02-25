package com.carolai.extractor.migration;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import com.carolai.extractor.migration.dto.MigrationOptions;
import com.carolai.extractor.persistence.entity.MigrationRunEntity;
import com.carolai.extractor.persistence.repository.MigrationRunRepository;

@Service
public class MigrationService {

    private static final Logger log = LogManager.getLogger(MigrationService.class);

    private final MigrationRunner runner;
    private final MigrationRunRepository runRepository;

    public MigrationService(MigrationRunner runner, MigrationRunRepository runRepository) {
        this.runner = runner;
        this.runRepository = runRepository;
    }

    public MigrationRunEntity runMigration(String type, MigrationOptions options) {
        log.info("Requested migration type={} options={}", type, options);

        return switch (type.toUpperCase()) {
            case "STUDENT" -> runner.runStudentMigration(options);
            case "EXERCISE" -> runner.runExerciseSeed(options);
            default -> throw new IllegalArgumentException("Unknown migration type: " + type);
        };
    }

    public List<MigrationRunEntity> getHistory() {
        return runRepository.findAllByOrderByStartedAtDesc();
    }
}
