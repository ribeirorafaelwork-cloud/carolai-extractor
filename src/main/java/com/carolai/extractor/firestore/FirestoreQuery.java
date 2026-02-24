package com.carolai.extractor.firestore;

import java.util.List;

public class FirestoreQuery {

    private final List<String> collections;
    private final List<FirestoreFilter> filters;

    public FirestoreQuery(List<String> collections, List<FirestoreFilter> filters) {
        this.collections = collections;
        this.filters = filters;
    }

    public List<String> getCollections() {
        return collections;
    }

    public List<FirestoreFilter> getFilters() {
        return filters;
    }
}