package com.carolai.extractor.dto.response;

public record FirestoreSubCollectionResponse<T>(
        T documents
) {}