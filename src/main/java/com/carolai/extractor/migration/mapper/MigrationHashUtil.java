package com.carolai.extractor.migration.mapper;

import org.apache.commons.codec.digest.DigestUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class MigrationHashUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private MigrationHashUtil() {}

    public static String sha256(Object payload) {
        try {
            String json = OBJECT_MAPPER.writeValueAsString(payload);
            return DigestUtils.sha256Hex(json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize payload for hashing", e);
        }
    }
}
