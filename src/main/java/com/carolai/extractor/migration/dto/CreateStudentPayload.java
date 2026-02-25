package com.carolai.extractor.migration.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CreateStudentPayload(
    String fullName,
    String email,
    String phone,
    String birthDate,
    String gender
) {}
