package com.carolai.extractor.dto.response;

import java.util.Map;

public record PhysicalAssessmentDto(
        Long id,
        Long customerId,
        String externalRef,
        Long assessmentCreatedAt,
        Long assessmentAnsweredAt,
        String createdAt,
        String lastModifiedAt,
        Map<String, Object> rawJson,
        String contentHash
) {}
