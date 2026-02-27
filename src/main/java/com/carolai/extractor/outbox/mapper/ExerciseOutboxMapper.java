package com.carolai.extractor.outbox.mapper;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class ExerciseOutboxMapper {

    public Map<String, Object> toCanonicalPayload(String exerciseName, String videoUrl) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", exerciseName);
        payload.put("videoUrl", videoUrl);
        return payload;
    }

    public String sourceKey(String exerciseName) {
        return "exercise:" + exerciseName.trim().toLowerCase().replace(" ", "_");
    }
}
