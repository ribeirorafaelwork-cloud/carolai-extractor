package com.carolai.extractor.outbox.mapper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import com.carolai.extractor.persistence.entity.CustomerEntity;

@Component
public class StudentOutboxMapper {

    private static final Logger log = LogManager.getLogger(StudentOutboxMapper.class);

    private static final DateTimeFormatter BR_DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter BR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public Map<String, Object> toCanonicalPayload(CustomerEntity customer) {
        Map<String, Object> payload = new LinkedHashMap<>();

        String fullName = customer.getName();
        if (fullName == null || fullName.isBlank()) {
            fullName = "Aluno Importado";
        }
        payload.put("fullName", fullName);

        String email = customer.getEmail();
        if (email == null || email.isBlank()) {
            email = "imported+" + customer.getExternalRef() + "@placeholder.local";
        }
        payload.put("email", email);

        payload.put("phone", customer.getPhone());
        payload.put("birthDate", parseBirthDate(customer.getBirthDate()));
        payload.put("gender", mapGender(customer.getGender()));

        return payload;
    }

    public String sourceKey(CustomerEntity customer) {
        String ref = customer.getExternalRef();
        return (ref != null && !ref.isBlank()) ? ref : "local-" + customer.getId();
    }

    private String mapGender(String raw) {
        if (raw == null || raw.isBlank()) {
            return "OTHER";
        }
        return switch (raw.toUpperCase().charAt(0)) {
            case 'M' -> "MALE";
            case 'F' -> "FEMALE";
            default -> "OTHER";
        };
    }

    private String parseBirthDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        // Try ISO format first (yyyy-MM-dd)
        try {
            LocalDate.parse(raw);
            return raw;
        } catch (DateTimeParseException ignored) {}

        // Try Brazilian format with time (dd/MM/yyyy HH:mm) — from FirestoreTimestamp.format()
        try {
            LocalDate date = LocalDate.parse(raw, BR_DATE_TIME);
            return date.toString();
        } catch (DateTimeParseException ignored) {}

        // Try Brazilian format date only (dd/MM/yyyy)
        try {
            LocalDate date = LocalDate.parse(raw, BR_DATE);
            return date.toString();
        } catch (DateTimeParseException e) {
            log.warn("Could not parse birthDate '{}', skipping", raw);
            return null;
        }
    }
}
