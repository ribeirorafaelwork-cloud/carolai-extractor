package com.carolai.extractor.outbox.mapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.carolai.extractor.persistence.entity.CustomerEntity;
import com.carolai.extractor.persistence.entity.ExerciseEntity;
import com.carolai.extractor.persistence.entity.GroupEntity;
import com.carolai.extractor.persistence.entity.SeriesEntity;
import com.carolai.extractor.persistence.entity.TrainingEntity;
import com.carolai.extractor.persistence.entity.TrainingPlanEntity;
import com.carolai.extractor.persistence.entity.TrainingPlanTrainingEntity;
import com.fasterxml.jackson.databind.JsonNode;

@Component
public class TrainingHistoryOutboxMapper {

    public List<Map<String, Object>> toCanonicalPayloads(CustomerEntity customer) {
        List<Map<String, Object>> results = new ArrayList<>();

        String studentEmail = customer.getEmail();
        if (studentEmail == null || studentEmail.isBlank()) {
            studentEmail = "imported+" + customer.getExternalRef() + "@placeholder.local";
        }

        for (TrainingPlanEntity plan : customer.getTrainingPlans()) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("studentEmail", studentEmail);
            payload.put("planName", plan.getName() != null ? plan.getName() : "Plano " + plan.getExternalRef());
            payload.put("startDate", plan.getStartAt());
            payload.put("endDate", plan.getEndAt());
            payload.put("active", plan.getActive());

            List<Map<String, Object>> sessions = new ArrayList<>();
            int dayIndex = 1;
            for (TrainingPlanTrainingEntity tpt : plan.getTrainingPlanTraining()) {
                TrainingEntity training = tpt.getTraining();
                if (training == null) continue;

                Map<String, Object> session = new LinkedHashMap<>();
                session.put("dayIndex", dayIndex++);
                session.put("seriesName", training.getName());
                session.put("notes", training.getNotes());

                List<Map<String, Object>> exercises = new ArrayList<>();
                List<GroupEntity> sortedGroups = training.getGroups().stream()
                        .sorted(Comparator.comparingInt(g -> g.getOrderIndex() != null ? g.getOrderIndex() : 0))
                        .toList();

                for (GroupEntity group : sortedGroups) {
                    List<SeriesEntity> sortedSeries = group.getSeries().stream()
                            .sorted(Comparator.comparingInt(s -> s.getOrderIndex() != null ? s.getOrderIndex() : 0))
                            .toList();

                    for (SeriesEntity series : sortedSeries) {
                        List<ExerciseEntity> sortedExercises = series.getExercises().stream()
                                .sorted(Comparator.comparingInt(e -> e.getOrderIndex() != null ? e.getOrderIndex() : 0))
                                .toList();

                        for (ExerciseEntity exercise : sortedExercises) {
                            Map<String, Object> ex = new LinkedHashMap<>();
                            ex.put("exerciseName", exercise.getExerciseName());
                            ex.put("groupName", group.getGroupName());
                            ex.put("notes", exercise.getNotes());
                            ex.put("videoUrl", exercise.getVideoUrl());
                            ex.put("orderIndex", exercise.getOrderIndex());

                            // Parse executionMode (Firestore arrayValue format)
                            // modoExecucao: arrayValue → values[] → mapValue → fields → {tipo, valor}
                            parseExecutionMode(exercise.getExecutionMode(), ex);

                            // Parse series exerciseInterval (Firestore fields format)
                            // intervaloSerie → fields → {restSeconds: integerValue}
                            JsonNode interval = series.getExerciseInterval();
                            if (interval != null && !interval.isNull() && interval.isObject()) {
                                Integer restSec = firestoreInt(interval, "restSeconds");
                                if (restSec != null) ex.put("restSeconds", restSec);
                            }

                            exercises.add(ex);
                        }
                    }
                }

                session.put("exercises", exercises);
                sessions.add(session);
            }

            payload.put("sessions", sessions);
            results.add(payload);
        }

        return results;
    }

    public String sourceKey(TrainingPlanEntity plan) {
        return plan.getCustomer().getExternalRef() + ":plan:" + plan.getExternalRef();
    }

    /**
     * Parses Firestore modoExecucao array to extract reps, weight, time, cadence.
     * Format: arrayValue → values[] → mapValue → fields → {tipo: stringValue, valor: stringValue|integerValue}
     * tipo values: Repetições → reps, Carga → weight, Tempo → durationSeconds, Cadência → cadence
     */
    private void parseExecutionMode(JsonNode execMode, Map<String, Object> target) {
        if (execMode == null || execMode.isNull()) return;

        JsonNode values = execMode.path("arrayValue").path("values");
        if (!values.isArray()) return;

        for (JsonNode entry : values) {
            JsonNode fields = entry.path("mapValue").path("fields");
            String tipo = firestoreString(fields, "tipo");
            String valor = firestoreAny(fields, "valor");
            if (tipo == null || valor == null || "0".equals(valor)) continue;

            switch (tipo) {
                case "Repetições" -> target.put("reps", valor);
                case "Carga" -> target.put("weight", valor);
                case "Tempo" -> target.put("durationSeconds", parseIntSafe(valor));
                case "Cadência" -> target.put("cadence", valor);
            }
        }
    }

    private static String firestoreString(JsonNode fields, String name) {
        JsonNode v = fields.path(name);
        if (v.has("stringValue")) return v.get("stringValue").asText();
        return null;
    }

    private static String firestoreAny(JsonNode fields, String name) {
        JsonNode v = fields.path(name);
        if (v.has("stringValue")) return v.get("stringValue").asText();
        if (v.has("integerValue")) return String.valueOf(v.get("integerValue").asLong());
        return null;
    }

    private static Integer firestoreInt(JsonNode fields, String name) {
        JsonNode v = fields.path(name);
        if (v.has("integerValue")) return v.get("integerValue").asInt();
        return null;
    }

    private static Integer parseIntSafe(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return null; }
    }

    private Integer intOrNull(JsonNode node, String field) {
        JsonNode child = node.get(field);
        if (child == null || child.isNull()) return null;
        if (child.isNumber()) return child.asInt();
        try { return Integer.parseInt(child.asText()); } catch (NumberFormatException e) { return null; }
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode child = node.get(field);
        if (child == null || child.isNull()) return null;
        return child.asText();
    }
}
