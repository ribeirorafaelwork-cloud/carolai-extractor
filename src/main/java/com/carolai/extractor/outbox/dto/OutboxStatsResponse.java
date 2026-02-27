package com.carolai.extractor.outbox.dto;

import java.util.Map;

public record OutboxStatsResponse(
        Map<String, Map<String, Long>> byTypeAndStatus,
        long totalPending,
        long totalAcked,
        long totalFailed
) {}
