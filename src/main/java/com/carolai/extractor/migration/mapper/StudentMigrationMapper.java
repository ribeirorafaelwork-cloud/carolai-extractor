package com.carolai.extractor.migration.mapper;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import com.carolai.extractor.migration.dto.CreateStudentPayload;
import com.carolai.extractor.persistence.entity.CustomerEntity;

@Component
public class StudentMigrationMapper {

    private static final Logger log = LogManager.getLogger(StudentMigrationMapper.class);

    public CreateStudentPayload toPayload(CustomerEntity customer) {
        String fullName = customer.getName();
        if (fullName == null || fullName.isBlank()) {
            fullName = "Aluno Importado";
        }

        String email = customer.getEmail();
        if (email == null || email.isBlank()) {
            email = "imported+" + customer.getExternalRef() + "@placeholder.local";
        }

        String birthDate = parseBirthDate(customer.getBirthDate());
        String gender = mapGender(customer.getGender());

        return new CreateStudentPayload(
                fullName,
                email,
                null,
                birthDate,
                gender
        );
    }

    public String sourceKey(CustomerEntity customer) {
        return customer.getExternalRef();
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
        try {
            LocalDate.parse(raw);
            return raw;
        } catch (DateTimeParseException e) {
            log.warn("Could not parse birthDate '{}', skipping", raw);
            return null;
        }
    }
}
