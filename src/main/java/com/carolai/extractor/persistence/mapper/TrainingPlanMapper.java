package com.carolai.extractor.persistence.mapper;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import com.carolai.extractor.dto.response.TrainingPlanResponse;
import com.carolai.extractor.persistence.entity.CustomerEntity;
import com.carolai.extractor.persistence.entity.TrainingPlanEntity;
import com.carolai.extractor.persistence.repository.CustomerRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class TrainingPlanMapper extends Mapper {

    private static final Logger log = LogManager.getLogger(TrainingPlanMapper.class);

    private final CustomerRepository customerRepo;

    private final ObjectMapper objectMapper;

    public TrainingPlanMapper(ObjectMapper objectMapper, CustomerRepository customerRepo) {
        this.objectMapper = objectMapper;
        this.customerRepo = customerRepo;
    }

    public TrainingPlanEntity toEntity(TrainingPlanResponse tpr) {
        TrainingPlanEntity tpe = new TrainingPlanEntity();

        if (tpr == null) {
            log.debug("⚠ TrainingPlanResponse is null");
            return tpe;
        }

        String customerRef = tpr.refAluno() != null ? tpr.refAluno().stringValue() : null;
        String trainingRef = tpr.refPlanoTreino() != null ? tpr.refPlanoTreino().stringValue() : null;

        log.debug("📥 Processing CustomerTraining | customerRef={} | trainingRef={}",
                customerRef, trainingRef);

        CustomerEntity customer = customerRepo.findByExternalRef(customerRef).orElse(null);

        if(customer == null) {
            log.debug(
                "❌ CustomerEntity is null for externalRef={} | tenant unknown | Firestore doc={}",
                customerRef,
                safeToJson(tpr)
            );
            return tpe;
        }

        tpe.setExternalRef(trainingRef);
        tpe.setCustomer(customer);

        tpe.setStartAt(
            tpr.dataInicioLong() != null ? tpr.dataInicioLong().integerValue() : null
        );
        tpe.setEndAt(
            tpr.dataTerminoLong() != null ? tpr.dataTerminoLong().integerValue() : null
        );
        tpe.setActive(
            tpr.planoTreinoAtivo() != null && tpr.planoTreinoAtivo().booleanValue()
        );
        tpe.setExecutionWeek(
            tpr.execucaoSemana() != null ? tpr.execucaoSemana().integerValue() : null
        );
        tpe.setPlannedSessions(
            tpr.treinosPrevistos() != null ? tpr.treinosPrevistos().integerValue() : null
        );
        tpe.setName(
            tpr.nomePlano() != null ? tpr.nomePlano().stringValue() : null
        );

        try {
            String json = objectMapper.writeValueAsString(tpr);
            tpe.setContentHash(DigestUtils.sha256Hex(json));
        } catch (JsonProcessingException e) {
            log.error(
                "⚠ Failed to generate content hash for CustomerTraining externalRef={}",
                trainingRef,
                e
            );
        }

        return tpe;
    }

    private String safeToJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "<serialization_failed>";
        }
    }

}