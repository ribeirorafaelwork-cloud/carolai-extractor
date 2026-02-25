package com.carolai.extractor.migration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PlatformLoginResult(
    boolean authenticated,
    PlatformTokens tokens
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PlatformTokens(
        String accessToken,
        String refreshToken
    ) {}
}
