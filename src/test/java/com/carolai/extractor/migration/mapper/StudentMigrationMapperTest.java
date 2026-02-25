package com.carolai.extractor.migration.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.carolai.extractor.migration.dto.CreateStudentPayload;
import com.carolai.extractor.persistence.entity.CustomerEntity;

class StudentMigrationMapperTest {

    private StudentMigrationMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new StudentMigrationMapper();
    }

    @Test
    void mapsGenderM_toMALE() {
        CustomerEntity c = customer("M");
        CreateStudentPayload payload = mapper.toPayload(c);
        assertEquals("MALE", payload.gender());
    }

    @Test
    void mapsGenderF_toFEMALE() {
        CustomerEntity c = customer("F");
        CreateStudentPayload payload = mapper.toPayload(c);
        assertEquals("FEMALE", payload.gender());
    }

    @Test
    void mapsGenderNull_toOTHER() {
        CustomerEntity c = customer(null);
        CreateStudentPayload payload = mapper.toPayload(c);
        assertEquals("OTHER", payload.gender());
    }

    @Test
    void mapsGenderEmpty_toOTHER() {
        CustomerEntity c = customer("");
        CreateStudentPayload payload = mapper.toPayload(c);
        assertEquals("OTHER", payload.gender());
    }

    @Test
    void mapsGenderUnknown_toOTHER() {
        CustomerEntity c = customer("X");
        CreateStudentPayload payload = mapper.toPayload(c);
        assertEquals("OTHER", payload.gender());
    }

    @Test
    void generatesPlaceholderEmailWhenNull() {
        CustomerEntity c = customer("M");
        c.setEmail(null);
        CreateStudentPayload payload = mapper.toPayload(c);
        assertTrue(payload.email().startsWith("imported+"));
        assertTrue(payload.email().endsWith("@placeholder.local"));
    }

    @Test
    void generatesPlaceholderEmailWhenBlank() {
        CustomerEntity c = customer("M");
        c.setEmail("  ");
        CreateStudentPayload payload = mapper.toPayload(c);
        assertTrue(payload.email().startsWith("imported+"));
    }

    @Test
    void preservesValidEmail() {
        CustomerEntity c = customer("F");
        c.setEmail("maria@test.com");
        CreateStudentPayload payload = mapper.toPayload(c);
        assertEquals("maria@test.com", payload.email());
    }

    @Test
    void parsesValidBirthDate() {
        CustomerEntity c = customer("M");
        c.setBirthDate("1990-05-15");
        CreateStudentPayload payload = mapper.toPayload(c);
        assertEquals("1990-05-15", payload.birthDate());
    }

    @Test
    void returnsNullForInvalidBirthDate() {
        CustomerEntity c = customer("M");
        c.setBirthDate("15/05/1990");
        CreateStudentPayload payload = mapper.toPayload(c);
        assertNull(payload.birthDate());
    }

    @Test
    void returnsNullForNullBirthDate() {
        CustomerEntity c = customer("M");
        c.setBirthDate(null);
        CreateStudentPayload payload = mapper.toPayload(c);
        assertNull(payload.birthDate());
    }

    @Test
    void usesDefaultNameWhenNull() {
        CustomerEntity c = customer("M");
        c.setName(null);
        CreateStudentPayload payload = mapper.toPayload(c);
        assertEquals("Aluno Importado", payload.fullName());
    }

    @Test
    void preservesValidName() {
        CustomerEntity c = customer("F");
        c.setName("Maria Silva");
        CreateStudentPayload payload = mapper.toPayload(c);
        assertEquals("Maria Silva", payload.fullName());
    }

    @Test
    void sourceKeyReturnsExternalRef() {
        CustomerEntity c = customer("M");
        assertEquals("ext-123", mapper.sourceKey(c));
    }

    @Test
    void payloadHashIsConsistent() {
        CustomerEntity c = customer("F");
        c.setName("Maria");
        c.setEmail("maria@test.com");
        c.setBirthDate("1990-01-01");

        CreateStudentPayload p1 = mapper.toPayload(c);
        CreateStudentPayload p2 = mapper.toPayload(c);

        assertEquals(MigrationHashUtil.sha256(p1), MigrationHashUtil.sha256(p2));
    }

    @Test
    void payloadHashChangesWhenDataChanges() {
        CustomerEntity c = customer("F");
        c.setName("Maria");
        c.setEmail("maria@test.com");
        CreateStudentPayload p1 = mapper.toPayload(c);

        c.setName("Maria Silva");
        CreateStudentPayload p2 = mapper.toPayload(c);

        assertNotNull(MigrationHashUtil.sha256(p1));
        assertNotNull(MigrationHashUtil.sha256(p2));
        assertTrue(!MigrationHashUtil.sha256(p1).equals(MigrationHashUtil.sha256(p2)));
    }

    private CustomerEntity customer(String gender) {
        CustomerEntity c = new CustomerEntity();
        c.setId(1L);
        c.setExternalRef("ext-123");
        c.setName("Test Name");
        c.setEmail("test@example.com");
        c.setGender(gender);
        return c;
    }
}
