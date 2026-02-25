package com.carolai.extractor.migration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PlatformStudentResponse(
    String id,
    String fullName,
    String email,
    String phone,
    String birthDate,
    String gender,
    String status
) {}
