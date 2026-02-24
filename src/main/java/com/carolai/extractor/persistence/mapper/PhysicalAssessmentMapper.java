package com.carolai.extractor.persistence.mapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import com.carolai.extractor.persistence.entity.CustomerEntity;
import com.carolai.extractor.persistence.entity.PhysicalAssessmentEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class PhysicalAssessmentMapper {

    private static final Logger log = LogManager.getLogger(PhysicalAssessmentMapper.class);

    private final ObjectMapper objectMapper;

    public PhysicalAssessmentMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public PhysicalAssessmentEntity toEntity(final Map<String, Object> assessment, final CustomerEntity customer) {
        final PhysicalAssessmentEntity entity = new PhysicalAssessmentEntity();

        final String normalizedJson = writeStableJson(assessment);
        final String contentHash = sha256Hex(normalizedJson);

        entity.setCustomerId(customer.getId());
        entity.setExternalRef(customer.getExternalRef());

        entity.setDocumentKey(asString(assessment.get("documentKey")));

        entity.setAssessmentCreatedAt(asLong(assessment.get("dataCriacaoAvaliacao")));
        entity.setAssessmentAnsweredAt(asLong(assessment.get("dataResposta")));
        entity.setCreatedAt(asString(assessment.get("dataCriacao")));
        entity.setLastModifiedAt(asString(assessment.get("dataUltimaModifiacao")));

        entity.setRawJson(buildFilteredRawJson(assessment));
        entity.setContentHash(contentHash);

        return entity;
    }

    private Map<String, Object> buildFilteredRawJson(final Map<String, Object> assessment) {
        final Object avaliacoesObj = assessment.get("avaliacoes");
        final Object alunoBIObj = assessment.get("alunoBI");

        final List<Map<String, Object>> avaliacoesFiltered = filterAvaliacoes(avaliacoesObj);

        final Map<String, Object> filtered = new LinkedHashMap<>();
        filtered.put("avaliacoes", avaliacoesFiltered);

        if (alunoBIObj != null) {
            filtered.put("alunoBI", alunoBIObj);
        }

        return filtered;
    }

    
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> filterAvaliacoes(final Object avaliacoesObj) {
        if (!(avaliacoesObj instanceof List<?> list)) {
            return Collections.emptyList();
        }

        return list.stream()
                .filter(it -> it instanceof Map)
                .map(it -> (Map<String, Object>) it)
                .map(this::removeUsuarioNode)
                .toList();
    }

    private Map<String, Object> removeUsuarioNode(final Map<String, Object> avaliacao) {
        final Map<String, Object> copy = new java.util.LinkedHashMap<>(avaliacao);
        copy.remove("usuario");
        return copy;
    }

    private static String asString(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private static Long asLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try {
            return Long.valueOf(String.valueOf(v));
        } catch (NumberFormatException e) {
            return null;
        }
    }


    private String writeStableJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar JSON para hash", e);
        }
    }

    private static String sha256Hex(String text) {
        try {
            final MessageDigest md = MessageDigest.getInstance("SHA-256");
            final byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
            final StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Falha ao gerar SHA-256", e);
        }
    }
}