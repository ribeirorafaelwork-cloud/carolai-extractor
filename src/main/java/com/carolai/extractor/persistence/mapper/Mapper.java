package com.carolai.extractor.persistence.mapper;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

@Component
public class Mapper {
    
    public static String ex(JsonNode n, String f) {
        JsonNode v = n.path(f);
        if (v.has("stringValue")) return v.get("stringValue").asText();
        if (v.has("nullValue")) return null;
        return null;
    }

    public static Integer exInt(JsonNode n, String f) {
        JsonNode v = n.path(f);
        return v.has("integerValue") ? v.get("integerValue").asInt() : null;
    }

    public static Long exLong(JsonNode n, String f) {
        JsonNode v = n.path(f);
        return v.has("integerValue") ? v.get("integerValue").asLong() : null;
    }

    public static Boolean exBool(JsonNode n, String f) {
        JsonNode v = n.path(f);
        return v.has("booleanValue") ? v.get("booleanValue").asBoolean() : null;
    }
}
