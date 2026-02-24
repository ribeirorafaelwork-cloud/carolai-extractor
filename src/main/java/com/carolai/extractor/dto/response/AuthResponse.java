package com.carolai.extractor.dto.response;

public record AuthResponse(
    String idToken,
    String refreshToken,
    String localId,
    String email
) {}