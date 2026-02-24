package com.carolai.extractor.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carolai.extractor.persistence.entity.CustomerEntity;
import com.carolai.extractor.persistence.entity.PhysicalAssessmentEntity;
import com.carolai.extractor.persistence.mapper.PhysicalAssessmentMapper;
import com.carolai.extractor.persistence.repository.PhysicalAssessmentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

@Service
public class PhysicalAssessmentService {

    private static final Logger log = LogManager.getLogger(PhysicalAssessmentService.class);

    private static final TypeReference<Map<String, Object>> MAP_REF = new TypeReference<>() {};
    private static final TypeReference<List<Map<String, Object>>> LIST_OF_MAP_REF = new TypeReference<>() {};

    private final PhysicalAssessmentRepository repo;
    private final ObjectMapper objectMapper;
    private final PhysicalAssessmentMapper mapper;

    public PhysicalAssessmentService(
            PhysicalAssessmentRepository repo,
            ObjectMapper objectMapper,
            PhysicalAssessmentMapper mapper
    ) {
        this.repo = repo;
        this.objectMapper = objectMapper.copy()
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
        this.mapper = mapper;
    }

    @Transactional
    public int parseAndUpsertFromRawJson(CustomerEntity customer, String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return 0;
        }

        final Map<String, Object> envelope = readAsMap(rawJson);

        final List<Map<String, Object>> sucesso = readListOfMap(envelope.get("sucesso"));
        if (sucesso.isEmpty()) {
            return 0;
        }

        int saved = 0;

        for (Map<String, Object> assessment : sucesso) {
            final String documentKey = asString(assessment.get("documentKey"));

            if (documentKey == null || documentKey.isBlank()) {
                log.warn("Assessment sem documentKey | customerId={} | externalRef={}",
                        customer.getId(), customer.getExternalRef());
                continue;
            }

            final Optional<PhysicalAssessmentEntity> existingOpt =
                    repo.findByCustomerIdAndDocumentKey(customer.getId(), documentKey);

            PhysicalAssessmentEntity entity = mapper.toEntity(assessment, customer);

            if (existingOpt.isPresent()
                && entity.getContentHash() != null
                && entity.getContentHash().equals(existingOpt.get().getContentHash())) {
                continue;
            }

            repo.save(entity);
            saved++;
        }

        return saved;
    }

     private Map<String, Object> readAsMap(String json) {
        try {
            return objectMapper.readValue(json, MAP_REF);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Falha ao parsear JSON de avaliação física", e);
        }
    }

    private List<Map<String, Object>> readListOfMap(Object value) {
        if (value == null) {
            return Collections.emptyList();
        }
        try {
            final String json = objectMapper.writeValueAsString(value);
            return objectMapper.readValue(json, LIST_OF_MAP_REF);
        } catch (JsonProcessingException e) {
            log.warn("Campo 'sucesso' não está no formato esperado: {}", value.getClass().getName(), e);
            return Collections.emptyList();
        }
    }

    private static String asString(Object v) {
        return v == null ? null : String.valueOf(v);
    }
}
