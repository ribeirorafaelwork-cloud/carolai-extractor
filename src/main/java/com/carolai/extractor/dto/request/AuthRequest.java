package com.carolai.extractor.dto.request;

public record AuthRequest(
        String email,
        String password,
        boolean returnSecureToken
) {}