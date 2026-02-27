package com.carolai.extractor.outbox.dto;

import java.util.Map;

public record OutboxItemResponse(
        Long id,
        String entityType,
        String sourceKey,
        Map<String, Object> payload,
        String payloadHash,
        String status,
        String createdAt
) {}
