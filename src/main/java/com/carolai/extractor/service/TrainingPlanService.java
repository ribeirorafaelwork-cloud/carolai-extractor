package com.carolai.extractor.service;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import com.carolai.extractor.config.FirestoreProperties;
import com.carolai.extractor.dto.OutcomeCounter;
import com.carolai.extractor.dto.response.FirestoreDocumentResponse;
import com.carolai.extractor.dto.response.RunQueryResponse;
import com.carolai.extractor.dto.response.TrainingPlanResponse;
import com.carolai.extractor.enums.Outcome;
import com.carolai.extractor.firestore.FirestoreQueryBuilder;
import com.carolai.extractor.persistence.entity.TrainingPlanEntity;
import com.carolai.extractor.persistence.mapper.TrainingPlanMapper;
import com.carolai.extractor.persistence.repository.TrainingPlanRepository;

@Service
public class TrainingPlanService {

    private static final Logger log = LogManager.getLogger(TrainingPlanService.class);

    private final FirestoreClientService firestore;
    private final FirestoreProperties props;
    private final TrainingPlanRepository trainingPlanRepository;
    private final TrainingPlanMapper trainingPlanMapper;

    public TrainingPlanService(FirestoreClientService firestore,
                           FirestoreProperties props,
                           TrainingPlanRepository trainingPlanRepository,
                           TrainingPlanMapper trainingPlanMapper) {
        this.firestore = firestore;
        this.props = props;
        this.trainingPlanRepository = trainingPlanRepository;
        this.trainingPlanMapper = trainingPlanMapper;
    }

    public void extractAndSave() {
        final String collection = props.getTrainingPlanCollection();

        log.debug("📡 Starting Firestore training plan extraction for personalId={} and collection={}",
                props.getPersonalId(), collection);

        var query = FirestoreQueryBuilder.create()
                .fromCollection(collection)
                .whereEquals("refClienteApp", props.getIdClientApp());

        List<RunQueryResponse<FirestoreDocumentResponse<TrainingPlanResponse>>> results;
        try {
            results = firestore.runQuery(collection, query, new ParameterizedTypeReference<>() {});
        } catch (Exception e) {
            log.error("❌ [API_ERROR] Falha ao buscar training plans do Firestore: {}", e.getMessage(), e);
            return;
        }

        if (results == null || results.isEmpty()) {
            log.debug("⚠ No training plan returned from Firestore.");
            return;
        }

        log.debug("📥 Firestore returned {} training plan documents.", results.size());

        OutcomeCounter counter = new OutcomeCounter();
        counter.setSize(results.size());

        for (RunQueryResponse<FirestoreDocumentResponse<TrainingPlanResponse>> r : results) {
            var outcome = processTrainingPlan(r);
            
            counter.increment(outcome);
        }

        log.info("""
                🎉 [TRAINING_PLAN_EXTRACTION]
                Source: Firestore
                Collection: {}
                Target: training_plan database
                --------------------------------
                Inserted: {}
                Updated:  {}
                Ignored:  {}
                Total processed: {}
                """,
                props.getTrainingPlanCollection(),
                counter.getInserted(),
                counter.getUpdated(),
                counter.getIgnored(),
                counter.getSize()
        );
    }

    private Outcome processTrainingPlan(RunQueryResponse<FirestoreDocumentResponse<TrainingPlanResponse>> response) {

        if (response.document() == null) {
            log.debug("⚠ Skipping entry: response contains no document.");
            return Outcome.SKIPPED;
        }

        var fields = response.document().fields();
        if (fields == null) {
            log.debug("⚠ Skipping entry: document has no fields.");
            return Outcome.SKIPPED;
        }

        try {
            TrainingPlanEntity incoming = trainingPlanMapper.toEntity(fields);

            if (incoming.getExternalRef() == null) {
                log.debug("❌ training plan skipped — refTreino (getExternalRef) is NULL.");
                return Outcome.SKIPPED;
            }

            return saveTrainingPlan(incoming);
        } catch (Exception e) {
            log.error("❌ [PERSISTENCE_ERROR] Falha ao inserir/atualizar TrainingPlan: {}", e.getMessage(), e);
            return Outcome.SKIPPED;
        }
    }

    private Outcome saveTrainingPlan(TrainingPlanEntity incoming) {
        TrainingPlanEntity existing = trainingPlanRepository.findByExternalRef(incoming.getExternalRef()).orElse(null);

        if (existing == null) {
            trainingPlanRepository.save(incoming);
            log.debug("🆕 INSERTED - TRAINING PLAN: {} INTO CUSTOMER: {} ({})", incoming.getName(),incoming.getCustomer().getName(), incoming.getId());
            return Outcome.INSERTED;
        }

        if (existing.getContentHash() != null &&
            existing.getContentHash().equals(incoming.getContentHash())) {

            log.debug("⏭️  IGNORED - (no changes): TRAINING PLAN: {} INTO CUSTOMER: {} ({})", incoming.getName(),incoming.getCustomer().getName(), incoming.getId());
            return Outcome.IGNORED;
        }

        incoming.setId(existing.getId());
        trainingPlanRepository.save(incoming);

        log.debug("🔄 UPDATED - TRAINING PLAN: {} INTO CUSTOMER: {} ({})", incoming.getName(), incoming.getCustomer().getName(), incoming.getId());
        return Outcome.UPDATED;
    }
}
