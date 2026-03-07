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
            List<TrainingPlanTrainingEntity> sortedTpts = plan.getTrainingPlanTraining().stream()
                    .sorted(Comparator.comparingLong(tpt -> tpt.getId() != null ? tpt.getId() : 0L))
                    .toList();
            for (TrainingPlanTrainingEntity tpt : sortedTpts) {
                TrainingEntity training = tpt.getTraining();
                if (training == null) continue;

                // Skip sessions not customized for the student (no treinos/ entry)
                if (tpt.getStudentExercisesJson() == null || tpt.getStudentExercisesJson().isNull()) continue;

                Map<String, Object> session = new LinkedHashMap<>();
                session.put("dayIndex", dayIndex++);
                // Name priority: student-customized > plan schedule > template
                String sessionName = firstNonBlank(
                        tpt.getStudentTrainingName(),
                        tpt.getName(),
                        training.getName()
                );
                session.put("seriesName", sessionName);
                session.put("trainingRef", training.getExternalRef());
                session.put("notes", training.getNotes());

                List<Map<String, Object>> exercises;
                if (tpt.getStudentExercisesJson() != null && !tpt.getStudentExercisesJson().isNull()) {
                    // Use student-customized exercises from treinos/ subcollection
                    exercises = parseExercisesFromFirestoreJson(tpt.getStudentExercisesJson());
                } else {
                    // Fallback to template exercises from TreinosLivres
                    exercises = buildExercisesFromEntities(training);
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
        String customerRef = plan.getCustomer() != null ? plan.getCustomer().getExternalRef() : null;
        String customerKey = (customerRef != null && !customerRef.isBlank())
                ? customerRef : "local-" + (plan.getCustomer() != null ? plan.getCustomer().getId() : "unknown");
        String planRef = plan.getExternalRef();
        String planKey = (planRef != null && !planRef.isBlank()) ? planRef : "local-" + plan.getId();
        return customerKey + ":plan:" + planKey;
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

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }

    /**
     * Builds exercise list from entity tree (TreinosLivres / template data).
     */
    private List<Map<String, Object>> buildExercisesFromEntities(TrainingEntity training) {
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

                    ex.put("seriesRef", series.getSeriesRef());
                    ex.put("seriesExerciseCount", sortedExercises.size());

                    parseExecutionMode(exercise.getExecutionMode(), ex);

                    JsonNode interval = series.getExerciseInterval();
                    if (interval != null && !interval.isNull() && interval.isObject()) {
                        Integer restSec = firestoreInt(interval, "restSeconds");
                        if (restSec != null) ex.put("restSeconds", restSec);
                    }

                    exercises.add(ex);
                }
            }
        }
        return exercises;
    }

    /**
     * Parses exercises directly from raw Firestore gruposExercicios JSON
     * (student-customized data from treinos/ subcollection).
     * Same structure as TreinosLivres: mapValue.fields.gruposLivres.arrayValue.values[...]
     */
    private List<Map<String, Object>> parseExercisesFromFirestoreJson(JsonNode gruposExercicios) {
        List<Map<String, Object>> exercises = new ArrayList<>();

        JsonNode groups = gruposExercicios
                .path("mapValue").path("fields")
                .path("gruposLivres")
                .path("arrayValue").path("values");

        if (!groups.isArray()) return exercises;

        for (JsonNode gNode : groups) {
            JsonNode g = gNode.path("mapValue").path("fields");
            if (g.isMissingNode()) continue;

            String groupName = firestoreString(g, "nomeGrupo");
            Integer groupOrder = firestoreIntField(g, "ordem");

            // Extract interval map for rest seconds
            java.util.Map<Integer, JsonNode> intervalByOrder = extractIntervalsFromJson(groups);

            JsonNode seriesNodes = g.path("seriesLivre")
                    .path("arrayValue").path("values");
            if (!seriesNodes.isArray()) continue;

            for (JsonNode sNode : seriesNodes) {
                JsonNode s = sNode.path("mapValue").path("fields");
                String seriesRef = firestoreString(s, "refSerie");
                Integer seriesOrder = firestoreIntField(s, "ordem");

                JsonNode intervalJson = seriesOrder != null ? intervalByOrder.get(seriesOrder) : null;

                JsonNode exerciseNodes = s.path("exerciciosLivre")
                        .path("arrayValue").path("values");
                if (!exerciseNodes.isArray()) continue;

                int exerciseCount = exerciseNodes.size();

                for (JsonNode exNode : exerciseNodes) {
                    JsonNode ex = exNode.path("mapValue").path("fields");

                    Map<String, Object> exerciseMap = new LinkedHashMap<>();
                    exerciseMap.put("exerciseName", firestoreString(ex, "nomeExercicio"));
                    exerciseMap.put("groupName", groupName);
                    exerciseMap.put("notes", firestoreString(ex, "observacao"));
                    exerciseMap.put("videoUrl", firestoreString(ex, "urlVideo"));
                    exerciseMap.put("orderIndex", firestoreIntField(ex, "ordem"));

                    exerciseMap.put("seriesRef", seriesRef);
                    exerciseMap.put("seriesExerciseCount", exerciseCount);

                    // Parse modoExecucao
                    parseExecutionMode(ex.path("modoExecucao"), exerciseMap);

                    // Parse rest interval
                    if (intervalJson != null && !intervalJson.isNull() && intervalJson.isObject()) {
                        Integer restSec = firestoreInt(intervalJson, "restSeconds");
                        if (restSec != null) exerciseMap.put("restSeconds", restSec);
                    }

                    exercises.add(exerciseMap);
                }
            }
        }
        return exercises;
    }

    private static Integer firestoreIntField(JsonNode fields, String name) {
        JsonNode v = fields.path(name);
        if (v.has("integerValue")) return v.get("integerValue").asInt();
        return null;
    }

    private java.util.Map<Integer, JsonNode> extractIntervalsFromJson(JsonNode groups) {
        java.util.Map<Integer, JsonNode> intervalByOrder = new java.util.HashMap<>();

        JsonNode intervalsArray = groups
                .path("intervaloSerie")
                .path("arrayValue").path("values");

        if (!intervalsArray.isArray()) return intervalByOrder;

        for (JsonNode intervalNode : intervalsArray) {
            JsonNode intervalFields = intervalNode.path("mapValue").path("fields");
            Integer ordem = firestoreIntField(intervalFields, "ordem");
            if (ordem != null) {
                intervalByOrder.put(ordem, intervalFields);
            }
        }

        return intervalByOrder;
    }
}
