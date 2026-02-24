package com.carolai.extractor.dto.response;

public record RunQueryResponse<T>(
        T document
) {}