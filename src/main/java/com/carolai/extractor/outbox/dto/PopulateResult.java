package com.carolai.extractor.outbox.dto;

public record PopulateResult(
        String entityType,
        int total,
        int inserted,
        int updated,
        int unchanged
) {}
