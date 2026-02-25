package com.carolai.extractor.migration;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.carolai.extractor.migration.dto.MigrationOptions;
import com.carolai.extractor.persistence.entity.MigrationRunEntity;

@RestController
@RequestMapping("/migration")
public class MigrationController {

    private final MigrationService migrationService;

    public MigrationController(MigrationService migrationService) {
        this.migrationService = migrationService;
    }

    @PostMapping("/run")
    public ResponseEntity<MigrationRunEntity> run(
            @RequestParam String type,
            @RequestParam(defaultValue = "false") boolean dryRun,
            @RequestParam(defaultValue = "0") int limit,
            @RequestParam(defaultValue = "true") boolean resume
    ) {
        var options = new MigrationOptions(dryRun, limit, resume);
        MigrationRunEntity result = migrationService.runMigration(type, options);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/runs")
    public ResponseEntity<List<MigrationRunEntity>> history() {
        return ResponseEntity.ok(migrationService.getHistory());
    }
}
