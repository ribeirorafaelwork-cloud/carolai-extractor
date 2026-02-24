package com.carolai.extractor.firestore;

import java.util.ArrayList;
import java.util.List;

public class FirestoreQueryBuilder {

    private String collection;
    private final List<FirestoreFilter> filters = new ArrayList<>();

    public static FirestoreQueryBuilder create() {
        return new FirestoreQueryBuilder();
    }

    public FirestoreQueryBuilder fromCollection(String collection) {
        this.collection = collection;
        return this;
    }

    public FirestoreQueryBuilder whereEquals(String field, String value) {
        this.filters.add(new FirestoreFilter(field, "EQUAL", value));
        return this;
    }

    public FirestoreQuery build() {
        return new FirestoreQuery(
                List.of(collection),
                filters
        );
    }

    public String toJson() {
        if (filters.isEmpty()) {
            throw new IllegalStateException("At least one filter is required");
        }

        if (filters.size() == 1) {
            FirestoreFilter f = filters.get(0);
            return singleFilterJson(f);
        }

        return compositeFilterJson(filters);
    }

    private String singleFilterJson(FirestoreFilter f) {
        return """
        {
          "structuredQuery": {
            "from": [ { "collectionId": "%s" } ],
            "where": {
              "fieldFilter": {
                "field": { "fieldPath": "%s" },
                "op": "%s",
                "value": { "stringValue": "%s" }
              }
            }
          }
        }
        """.formatted(collection, f.fieldPath(), f.op(), f.value());
    }

    private String compositeFilterJson(List<FirestoreFilter> filters) {
        StringBuilder filtersJson = new StringBuilder();

        for (int i = 0; i < filters.size(); i++) {
            FirestoreFilter f = filters.get(i);

            filtersJson.append("""
            {
              "fieldFilter": {
                "field": { "fieldPath": "%s" },
                "op": "%s",
                "value": { "stringValue": "%s" }
              }
            }
            """.formatted(f.fieldPath(), f.op(), f.value()));

            if (i < filters.size() - 1) {
                filtersJson.append(",");
            }
        }

        return """
        {
          "structuredQuery": {
            "from": [ { "collectionId": "%s" } ],
            "where": {
              "compositeFilter": {
                "op": "AND",
                "filters": [
                  %s
                ]
              }
            }
          }
        }
        """.formatted(collection, filtersJson.toString());
    }

}