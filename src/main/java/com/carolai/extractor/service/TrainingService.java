package com.carolai.extractor.service;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import com.carolai.extractor.config.FirestoreProperties;
import com.carolai.extractor.dto.OutcomeCounter;
import com.carolai.extractor.dto.response.FirestoreDocumentResponse;
import com.carolai.extractor.dto.response.FreeTrainingFieldsResponse;
import com.carolai.extractor.dto.response.RunQueryResponse;
import com.carolai.extractor.enums.Outcome;
import com.carolai.extractor.firestore.FirestoreQueryBuilder;
import com.carolai.extractor.persistence.entity.TrainingEntity;
import com.carolai.extractor.persistence.mapper.TrainingMapper;
import com.carolai.extractor.persistence.repository.TrainingRepository;

@Service
public class TrainingService {

    private static final Logger log = LogManager.getLogger(TrainingService.class);

    private final FirestoreClientService firestore;
    private final FirestoreProperties props;
    private final TrainingRepository trainingRepo;
    private final TrainingMapper trainingMapper;

    public TrainingService(FirestoreClientService firestore,
                           FirestoreProperties props,
                           TrainingRepository trainingRepo,
                           TrainingMapper trainingMapper) {
        this.firestore = firestore;
        this.props = props;
        this.trainingRepo = trainingRepo;
        this.trainingMapper = trainingMapper;
    }

    public void extractAndSave() {
        final String collection = props.getTrainingCollection();

        log.debug("📡 Starting Firestore training extraction for personalId={} and collection={}",
                props.getPersonalId(), collection);

        var query = FirestoreQueryBuilder.create()
                .fromCollection(collection)
                .whereEquals("refPersonalMontou", props.getPersonalId());

        List<RunQueryResponse<FirestoreDocumentResponse<FreeTrainingFieldsResponse>>> results = 
                firestore.runQuery(collection, query, new ParameterizedTypeReference<>() {});

        if (results == null || results.isEmpty()) {
            log.warn("⚠ No trainings returned from Firestore.");
            return;
        }

        log.debug("📥 Firestore returned {} training documents.", results.size());

        OutcomeCounter counter = new OutcomeCounter();
        counter.setSize(results.size());

        for (RunQueryResponse<FirestoreDocumentResponse<FreeTrainingFieldsResponse>>  r : results) {
            var outcome = processTraining(r);

            counter.increment(outcome);
        }

        log.info("""
                🎉 [TRAINING_EXTRACTION]
                Source: Firestore
                Collection: {}
                Target: training database
                --------------------------------
                Inserted: {}
                Updated:  {}
                Ignored:  {}
                Total processed: {}
                """,
                props.getTrainingCollection(),
                counter.getInserted(),
                counter.getUpdated(),
                counter.getIgnored(),
                counter.getSize()
        );
    }

    private Outcome processTraining(RunQueryResponse<FirestoreDocumentResponse<FreeTrainingFieldsResponse>> response) {

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
            TrainingEntity incoming = trainingMapper.toEntity(fields);

            if (incoming.getExternalRef() == null) {
                log.debug("❌ Training skipped — refTreino (getExternalRef) is NULL.");
                return Outcome.SKIPPED;
            }

            return saveTraining(incoming);

        } catch (Exception e) {
            log.error("❌ Failed to process training document. {}", e.getMessage(), e);
            return Outcome.SKIPPED;
        }
    }

    private Outcome saveTraining(TrainingEntity incoming) {

        TrainingEntity existing = trainingRepo.findByExternalRef(incoming.getExternalRef()).orElse(null);

        if (existing == null) {
            trainingRepo.save(incoming);
            log.debug("🆕 INSERTED: {} ({})", incoming.getName(), incoming.getId());
            return Outcome.INSERTED;
        }

        if (existing.getContentHash() != null &&
            existing.getContentHash().equals(incoming.getContentHash())) {

            log.debug("⏭️  IGNORED (no changes): {} ({})", incoming.getName(), incoming.getId());
            return Outcome.IGNORED;
        }

        incoming.setId(existing.getId());
        trainingRepo.save(incoming);

        log.debug("🔄 UPDATED: {} ({})", incoming.getName(), incoming.getId());
        return Outcome.UPDATED;
    }
}
