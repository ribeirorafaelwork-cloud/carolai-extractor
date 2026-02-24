package com.carolai.extractor.persistence.mapper;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import com.carolai.extractor.dto.response.FreeTrainingFieldsResponse;
import com.carolai.extractor.persistence.entity.ExerciseEntity;
import com.carolai.extractor.persistence.entity.GroupEntity;
import com.carolai.extractor.persistence.entity.SeriesEntity;
import com.carolai.extractor.persistence.entity.TrainingEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class TrainingMapper extends Mapper {

    private static final Logger log = LogManager.getLogger(TrainingMapper.class);

    private final ObjectMapper objectMapper;

    public TrainingMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public TrainingEntity toEntity(FreeTrainingFieldsResponse f) {
        TrainingEntity t = new TrainingEntity();

        if (f == null) {
            log.warn("⚠ FreeTrainingFieldsResponse is null");
            return t;
        }

        try {
            mapTrainingCore(t, f);
            mapGroups(t, f);

        } catch (JsonProcessingException ex) {
            log.error("❌ Error TrainingMapper: {}", ex.getMessage(), ex);
        }

        return t;
    }

    private void mapTrainingCore(TrainingEntity t, FreeTrainingFieldsResponse f)
            throws JsonProcessingException {

        t.setExternalRef(f.refTreino().stringValue());
        t.setName(f.nomeTreino() != null ? f.nomeTreino().stringValue() : null);
        t.setPersonalId(f.refPersonalMontou() != null ? f.refPersonalMontou().stringValue() : null);
        t.setPersonalName(f.nomePersonalMontou() != null ? f.nomePersonalMontou().stringValue() : null);
        t.setCoverUrl(f.urlCapa() != null ? f.urlCapa().stringValue() : null);
        t.setNotes(f.observacoes() != null ? f.observacoes().stringValue() : null);

        t.setCreatedAt(f.dataCriacao() != null ? f.dataCriacao().format() : null);

        try {
            String json = objectMapper.writeValueAsString(f);
            t.setContentHash(DigestUtils.sha256Hex(json));
        } catch (JsonProcessingException e) {
            log.warn("⚠ Failed to generate content hash for Training {}", f.refTreino(), e);
        }
    }

    private void mapGroups(TrainingEntity t, FreeTrainingFieldsResponse f) {

        if (f.gruposExercicios() == null) {
            return;
        }

        JsonNode root = objectMapper.valueToTree(f.gruposExercicios());

        JsonNode groups = root
                .path("mapValue").path("fields")
                .path("gruposLivres")
                .path("arrayValue").path("values");

        if (!groups.isArray()) {
            return;
        }

        for (JsonNode gNode : groups) {
            GroupEntity group = mapGroup(gNode, groups);
            if (group != null) {
                t.addGroup(group);
            }
        }
    }

    private GroupEntity mapGroup(JsonNode gNode, JsonNode allGroups) {

        JsonNode g = gNode.path("mapValue").path("fields");
        if (g.isMissingNode()) return null;

        GroupEntity group = new GroupEntity();
        group.setGroupRef(ex(g, "refGrupo"));
        group.setGroupName(ex(g, "nomeGrupo"));
        group.setOrderIndex(exInt(g, "ordem"));

        Map<Integer, JsonNode> intervalByOrder = extractIntervals(allGroups);

        mapSeries(group, g, intervalByOrder);
        return group;
    }

    private void mapSeries(
            GroupEntity group,
            JsonNode g,
            Map<Integer, JsonNode> intervalByOrder
    ) {

        JsonNode seriesNodes = g.path("seriesLivre")
                .path("arrayValue").path("values");

        if (!seriesNodes.isArray()) return;

        for (JsonNode sNode : seriesNodes) {
            SeriesEntity series = new SeriesEntity();
            JsonNode s = sNode.path("mapValue").path("fields");

            series.setSeriesRef(ex(s, "refSerie"));
            series.setOrderIndex(exInt(s, "ordem"));

            JsonNode intervalJson = intervalByOrder.get(series.getOrderIndex());
            series.setExerciseInterval(intervalJson);

            mapExercises(series, s);
            group.addSeries(series);
        }
    }

    private void mapExercises(SeriesEntity series, JsonNode s) {

        JsonNode exercises = s.path("exerciciosLivre")
                .path("arrayValue").path("values");

        if (!exercises.isArray()) return;

        for (JsonNode exNode : exercises) {
            ExerciseEntity e = new ExerciseEntity();
            JsonNode ex = exNode.path("mapValue").path("fields");

            e.setExerciseRef(ex(ex, "refExercicio"));
            e.setExerciseName(ex(ex, "nomeExercicio"));
            e.setNotes(ex(ex, "observacao"));
            e.setVideoUrl(ex(ex, "urlVideo"));
            e.setCoverUrl(ex(ex, "urlCapa"));
            e.setFreeExecution(exBool(ex, "livre"));
            e.setInHome(exBool(ex, "emCasa"));
            e.setOrderIndex(exInt(ex, "ordem"));
            e.setCreatedAt(exLong(ex, "dataCriado"));

            e.setEquipments(ex.path("modoExecucao"));
            e.setEquipments(ex.path("gruposMusculares"));
            e.setEquipments(ex.path("equipamentos"));

            series.addExercise(e);
        }
    }

    private Map<Integer, JsonNode> extractIntervals(JsonNode groups) {

        Map<Integer, JsonNode> intervalByOrder = new HashMap<>();

        JsonNode intervalsArray = groups
                .path("intervaloSerie")
                .path("arrayValue").path("values");

        if (!intervalsArray.isArray()) {
            return intervalByOrder;
        }

        for (JsonNode intervalNode : intervalsArray) {
            JsonNode intervalFields = intervalNode.path("mapValue").path("fields");
            Integer ordem = exInt(intervalFields, "ordem");
            if (ordem != null) {
                intervalByOrder.put(ordem, intervalFields);
            }
        }

        return intervalByOrder;
    }
}