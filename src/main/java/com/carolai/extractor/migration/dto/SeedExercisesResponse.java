package com.carolai.extractor.migration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SeedExercisesResponse(
    int added
) {}
