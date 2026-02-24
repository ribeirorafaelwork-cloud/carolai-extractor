package com.carolai.extractor.dto.response;

public record FirestoreDocumentResponse<T>(
        String name,
        T fields,
        String createTime,
        String updateTime
) {}