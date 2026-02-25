package com.carolai.extractor.migration.dto;

public record PlatformLoginRequest(
    String email,
    String password,
    String tenantId
) {}
