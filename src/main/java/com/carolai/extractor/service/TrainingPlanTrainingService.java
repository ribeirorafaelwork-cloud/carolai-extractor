package com.carolai.extractor.service;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import com.carolai.extractor.config.FirestoreProperties;
import com.carolai.extractor.dto.OutcomeCounter;
import com.carolai.extractor.dto.response.FirestoreDocumentResponse;
import com.carolai.extractor.dto.response.FirestoreSubCollectionResponse;
import com.carolai.extractor.dto.response.TrainingPlanTrainingFieldsResponse;
import com.carolai.extractor.enums.Outcome;
import com.carolai.extractor.persistence.entity.TrainingPlanEntity;
import com.carolai.extractor.persistence.entity.TrainingPlanTrainingEntity;
import com.carolai.extractor.persistence.mapper.TrainingPlanTrainingMapper;
import com.carolai.extractor.persistence.repository.TrainingPlanRepository;
import com.carolai.extractor.persistence.repository.TrainingPlanTrainingRepository;

@Service
public class TrainingPlanTrainingService {

    private static final Logger log = LogManager.getLogger(TrainingPlanTrainingService.class);

    private final FirestoreClientService firestore;
    private final FirestoreProperties props;
    private final TrainingPlanRepository trainingPlanRepository;
    private final TrainingPlanTrainingRepository trainingPlanTrainingRepository;
    private final TrainingPlanTrainingMapper trainingPlanTrainingMapper;

    public TrainingPlanTrainingService(FirestoreClientService firestore,
                           FirestoreProperties props,
                           TrainingPlanRepository trainingPlanRepository,
                           TrainingPlanTrainingRepository trainingPlanTrainingRepository,
                           TrainingPlanTrainingMapper trainingPlanTrainingMapper) {
        this.firestore = firestore;
        this.props = props;
        this.trainingPlanRepository = trainingPlanRepository;
        this.trainingPlanTrainingRepository = trainingPlanTrainingRepository;
        this.trainingPlanTrainingMapper = trainingPlanTrainingMapper;
    }

    private static final int PARALLELISM = 10;

    public void extractAndSave() {
        List<TrainingPlanEntity> trainingPlans = trainingPlanRepository.findAll();

        OutcomeCounter counter = new OutcomeCounter();
        AtomicInteger progress = new AtomicInteger(0);
        int total = trainingPlans.size();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            java.util.concurrent.Semaphore semaphore = new java.util.concurrent.Semaphore(PARALLELISM);

            for (TrainingPlanEntity trainingPlan : trainingPlans) {
                semaphore.acquire();
                executor.submit(() -> {
                    try {
                        processTrainingPlanTrainings(trainingPlan, counter);
                        int done = progress.incrementAndGet();
                        if (done % 100 == 0) {
                            log.info("[TRAINING_PLAN_TRAINING] Progress: {}/{}", done, total);
                        }
                    } finally {
                        semaphore.release();
                    }
                });
            }

            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[TRAINING_PLAN_TRAINING] Interrupted during parallel extraction");
        }

        log.info("""
                [TRAINING_PLAN_TRAINING_EXTRACTION]
                Source: Firestore
                Collection: {}
                Target: training_plan_training database
                --------------------------------
                Inserted:   {}
                Updated:    {}
                Ignored:    {}
                API Errors: {}
                Total processed: {}
                """,
                props.getTrainingPlanTrainingSubcollection(),
                counter.getInserted(),
                counter.getUpdated(),
                counter.getIgnored(),
                counter.getApiErrors(),
                counter.getSize()
        );
    }

    private void processTrainingPlanTrainings(TrainingPlanEntity trainingPlan, OutcomeCounter counter) {
        final String collection = props.getTrainingPlanCollection();
        final String subCollection = props.getTrainingPlanTrainingSubcollection();
        final String planoRef = trainingPlan.getExternalRef();

        FirestoreSubCollectionResponse<List<FirestoreDocumentResponse<TrainingPlanTrainingFieldsResponse>>> doc;
        try {
            doc = firestore.listSubcollection(
                collection + "/" + planoRef,
                subCollection,
                new ParameterizedTypeReference<
                    FirestoreSubCollectionResponse<List<FirestoreDocumentResponse<TrainingPlanTrainingFieldsResponse>>>
                >() {}
            );
        } catch (Exception e) {
            log.warn("⚠ [API_ERROR] Falha ao buscar subcoleção do Firestore para trainingPlan={}|externalRef={}: {}",
                    trainingPlan.getId(), planoRef, e.getMessage());
            counter.increment(Outcome.API_ERROR);
            return;
        }

        if (doc == null || doc.documents() == null || doc.documents().isEmpty()) {
            log.debug("⚠ No treinosPreDefinidos returned from Firestore.");
            return;
        }

        counter.setSize(doc.documents().size());

        for (FirestoreDocumentResponse<TrainingPlanTrainingFieldsResponse> docItem : doc.documents()) {
            var outcome = processTrainingPlanTraining(docItem.fields(), trainingPlan);

            counter.increment(outcome);
        }
    }

    private Outcome processTrainingPlanTraining(TrainingPlanTrainingFieldsResponse fields, TrainingPlanEntity trainingPlan) {

        if (fields == null) {
            log.debug("⚠ Skipping entry: fields is null.");
            return Outcome.SKIPPED;
        }

        try {
            TrainingPlanTrainingEntity incoming = trainingPlanTrainingMapper.toEntity(fields);

            if (incoming.getTraining() == null) {
                log.debug("❌ training plan training skipped — refTreino (getExternalRef) is NULL.");
                return Outcome.SKIPPED;
            }

            incoming.setTrainingPlan(trainingPlan);
            return saveTrainingPlanTraining(incoming);
        } catch (Exception e) {
            log.error("❌ [PERSISTENCE_ERROR] Falha ao inserir/atualizar TrainingPlanTraining: {}", e.getMessage(), e);
            return Outcome.SKIPPED;
        }
    }

    private Outcome saveTrainingPlanTraining(TrainingPlanTrainingEntity incoming) {
        final String refTreino = incoming.getTraining().getExternalRef();
        final String refPlanTraining = incoming.getTrainingPlan().getExternalRef();

        TrainingPlanTrainingEntity existing = trainingPlanTrainingRepository.findByRefs(refTreino, refPlanTraining).orElse(null);
        
        if (existing == null) {
            trainingPlanTrainingRepository.save(incoming);
            log.debug("🆕 INSERTED - TRAINING PLAN: {} INTO CUSTOMER: {} ({})", refTreino, refPlanTraining, incoming.getId());
            return Outcome.INSERTED;
        }

        if (existing.getContentHash() != null &&
            existing.getContentHash().equals(incoming.getContentHash())) {
            log.debug("⏭️ IGNORED - TRAINING PLAN: {} INTO CUSTOMER: {} ({})", refTreino, refPlanTraining, incoming.getId());
            return Outcome.IGNORED;
        }

        incoming.setId(existing.getId());
        trainingPlanTrainingRepository.save(incoming);

        log.debug("🔄 UPDATED - TRAINING PLAN: {} INTO CUSTOMER: {} ({})", refTreino, refPlanTraining, incoming.getId());
        return Outcome.UPDATED;
    }
}
