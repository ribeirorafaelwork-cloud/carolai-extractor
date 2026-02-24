package com.carolai.extractor.persistence.mapper;

import java.time.Instant;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import com.carolai.extractor.dto.response.CustomerResponse;
import com.carolai.extractor.persistence.entity.CustomerEntity;
import com.carolai.extractor.persistence.entity.CustomerObjectiveEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class CustomerMapper extends Mapper {

    private static final Logger log = LogManager.getLogger(CustomerMapper.class);

    private final ObjectMapper objectMapper;

    public CustomerMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public CustomerEntity toEntity(CustomerResponse f) {
        CustomerEntity c = new CustomerEntity();

        if (f == null) {
            log.debug("⚠ CustomerResponse is null");
            return c;
        }
        c.setExternalRef(f.ref() != null ? f.ref().stringValue() : null);

        c.setName(f.nome() != null ? f.nome().stringValue() : null);
        c.setEmail(f.email() != null ? f.email().stringValue() : null);
        c.setGender(f.sexo() != null ? f.sexo().stringValue() : null);
        c.setDoc(f.cpf() != null ? f.cpf().stringValue() : null);

        c.setBirthDate(f.dataNascimento() != null
                ? f.dataNascimento().format()
                : null);

        c.setCreatedAt(f.dataCadastro() != null
                ? f.dataCadastro().format()
                : null);

        c.setUpdatedAt(f.dataAtualizacao() != null
                ? f.dataAtualizacao().format()
                : null);

        try {
            String json = objectMapper.writeValueAsString(f);
            c.setContentHash(DigestUtils.sha256Hex(json));
        } catch (JsonProcessingException e) {
            log.error("⚠ Failed to generate content hash for customer {}", f.ref(), e);
        }

        addObjectives(f, c);

        return c;
    }

    public void addObjectives(CustomerResponse f, CustomerEntity c) {
        JsonNode objetivosNode = objectMapper.valueToTree(f.objetivos());
        JsonNode values = objetivosNode
                .path("arrayValue")
                .path("values");

        log.debug("📌 Found {} objectives for customerId={}",
                values.size(),
                f.ref() != null ? f.ref().stringValue() : "UNKNOWN");

        for (JsonNode objNode : values) {

            JsonNode fields = objNode
                    .path("mapValue")
                    .path("fields");

            if (fields.isMissingNode()) {
                continue;
            }

            CustomerObjectiveEntity o = new CustomerObjectiveEntity();

            o.setObjectiveCode(exInt(fields, "codigo"));
            o.setName(ex(fields, "nome"));
            o.setNameNormalized(ex(fields, "nomeMin"));
            o.setSelected(exBool(fields, "selecionado"));
            o.setSource("IMPORT");
            o.setSnapshotAt(Instant.now());
            o.setCustomer(c);

            c.addObjective(o);
        }
    }



}