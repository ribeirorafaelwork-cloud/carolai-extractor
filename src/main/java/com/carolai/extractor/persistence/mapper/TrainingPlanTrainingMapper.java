package com.carolai.extractor.persistence.mapper;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import com.carolai.extractor.dto.response.TrainingPlanTrainingFieldsResponse;
import com.carolai.extractor.persistence.entity.TrainingEntity;
import com.carolai.extractor.persistence.entity.TrainingPlanTrainingEntity;
import com.carolai.extractor.persistence.repository.TrainingRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class TrainingPlanTrainingMapper extends Mapper {

    private static final Logger log = LogManager.getLogger(TrainingPlanTrainingMapper.class);

    private final TrainingRepository trainingRepository;

    private final ObjectMapper objectMapper;

    public TrainingPlanTrainingMapper(ObjectMapper objectMapper, TrainingRepository trainingRepository) {
        this.objectMapper = objectMapper;
        this.trainingRepository = trainingRepository;
    }

    public TrainingPlanTrainingEntity toEntity(TrainingPlanTrainingFieldsResponse tptr) {
        TrainingPlanTrainingEntity tpr = new TrainingPlanTrainingEntity();

        if (tptr == null) {
            log.debug("⚠ TrainingPlanTrainingFieldsResponse is null");
            return tpr;
        }

        final String refTreino = tptr.refTreino().stringValue();

        log.debug("📥 Processing TrainingPlanTraining | trainingRef={}", refTreino);

        TrainingEntity training = trainingRepository.findByExternalRef(refTreino).orElse(null);

        if(training == null) {
            log.debug(
                "❌ TrainingEntity is null for externalRef={} | tenant unknown | Firestore doc={}",
                refTreino,
                safeToJson(tptr)
            );
            return tpr;
        }

        tpr.setTraining(training);

        try {
            String json = objectMapper.writeValueAsString(tptr);
            tpr.setContentHash(DigestUtils.sha256Hex(json));
        } catch (JsonProcessingException e) {
            log.error(
                "⚠ Failed to generate content hash for TrainingPlanTraining externalRef={}",
                refTreino,
                e
            );
        }

        return tpr;
    }

    private String safeToJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "<serialization_failed>";
        }
    }

}