package com.carolai.extractor.migration.dto;

public record MigrationOptions(
    boolean dryRun,
    int limit,
    boolean resume
) {
    public static MigrationOptions defaults() {
        return new MigrationOptions(false, 0, true);
    }
}
