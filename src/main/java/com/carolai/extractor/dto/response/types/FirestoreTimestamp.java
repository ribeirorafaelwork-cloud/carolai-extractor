package com.carolai.extractor.dto.response.types;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public record FirestoreTimestamp(String timestampValue) {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                    .withZone(ZoneId.systemDefault());

    public String format() {
        if (timestampValue == null || timestampValue.isBlank()) {
            return null;
        }

        try {
            Instant instant = Instant.parse(timestampValue);
            return FORMATTER.format(instant);
        } catch (Exception e) {
            return null;
        }
    }
}
