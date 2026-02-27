package com.carolai.extractor.outbox.mapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.carolai.extractor.persistence.entity.CustomerEntity;
import com.carolai.extractor.persistence.entity.CustomerObjectiveEntity;

@Component
public class ObjectiveOutboxMapper {

    public Map<String, Object> toCanonicalPayload(CustomerEntity customer) {
        Map<String, Object> payload = new LinkedHashMap<>();

        String studentEmail = customer.getEmail();
        if (studentEmail == null || studentEmail.isBlank()) {
            studentEmail = "imported+" + customer.getExternalRef() + "@placeholder.local";
        }
        payload.put("studentEmail", studentEmail);

        List<Map<String, Object>> objectives = new ArrayList<>();
        List<CustomerObjectiveEntity> sorted = customer.getObjectives().stream()
                .sorted(Comparator.comparingInt(o -> o.getObjectiveCode() != null ? o.getObjectiveCode() : 0))
                .toList();
        for (CustomerObjectiveEntity obj : sorted) {
            Map<String, Object> o = new LinkedHashMap<>();
            o.put("code", obj.getObjectiveCode());
            o.put("name", obj.getName());
            o.put("selected", obj.getSelected());
            objectives.add(o);
        }
        payload.put("objectives", objectives);

        // Use the most recent snapshotAt from the objectives
        String snapshotAt = customer.getObjectives().stream()
                .filter(o -> o.getSnapshotAt() != null)
                .map(o -> o.getSnapshotAt().toString())
                .max(String::compareTo)
                .orElse(null);
        payload.put("snapshotAt", snapshotAt);

        return payload;
    }

    public String sourceKey(CustomerEntity customer) {
        String ref = customer.getExternalRef();
        String key = (ref != null && !ref.isBlank()) ? ref : "local-" + customer.getId();
        return key + ":objectives";
    }
}
