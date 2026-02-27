package com.carolai.extractor.outbox.mapper;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.carolai.extractor.persistence.entity.CustomerEntity;
import com.carolai.extractor.persistence.entity.PhysicalAssessmentEntity;

@Component
public class PhysicalAssessmentOutboxMapper {

    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ISO_INSTANT;

    public Map<String, Object> toCanonicalPayload(PhysicalAssessmentEntity assessment, CustomerEntity customer) {
        Map<String, Object> payload = new LinkedHashMap<>();

        String studentEmail = customer.getEmail();
        if (studentEmail == null || studentEmail.isBlank()) {
            studentEmail = "imported+" + customer.getExternalRef() + "@placeholder.local";
        }
        payload.put("studentEmail", studentEmail);

        payload.put("documentKey", assessment.getDocumentKey());
        payload.put("assessmentDate", epochMillisToIso(assessment.getAssessmentCreatedAt()));
        payload.put("answeredDate", epochMillisToIso(assessment.getAssessmentAnsweredAt()));
        payload.put("data", assessment.getRawJson());

        return payload;
    }

    public String sourceKey(PhysicalAssessmentEntity assessment) {
        return assessment.getCustomerId() + ":assessment:" + assessment.getDocumentKey();
    }

    private String epochMillisToIso(Long epochMillis) {
        if (epochMillis == null) return null;
        return Instant.ofEpochMilli(epochMillis).atOffset(ZoneOffset.UTC).format(ISO_FMT);
    }
}
