package com.carolai.extractor.firestore;

public record FirestoreFilter(
        String fieldPath,
        String op,
        String value
) {}