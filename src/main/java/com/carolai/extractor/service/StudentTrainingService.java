package com.carolai.extractor.service;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
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
import com.carolai.extractor.dto.response.FreeTrainingFieldsResponse;
import com.carolai.extractor.enums.Outcome;
import com.carolai.extractor.persistence.entity.TrainingPlanEntity;
import com.carolai.extractor.persistence.repository.TrainingPlanRepository;
import com.carolai.extractor.persistence.repository.TrainingPlanTrainingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.transaction.support.TransactionTemplate;

@Service
public class StudentTrainingService {

    private static final Logger log = LogManager.getLogger(StudentTrainingService.class);

    private final FirestoreClientService firestore;
    private final FirestoreProperties props;
    private final TrainingPlanRepository trainingPlanRepository;
    private final TrainingPlanTrainingRepository trainingPlanTrainingRepository;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate txTemplate;

    public StudentTrainingService(FirestoreClientService firestore,
                                  FirestoreProperties props,
                                  TrainingPlanRepository trainingPlanRepository,
                                  TrainingPlanTrainingRepository trainingPlanTrainingRepository,
                                  ObjectMapper objectMapper,
                                  TransactionTemplate txTemplate) {
        this.firestore = firestore;
        this.props = props;
        this.trainingPlanRepository = trainingPlanRepository;
        this.trainingPlanTrainingRepository = trainingPlanTrainingRepository;
        this.objectMapper = objectMapper;
        this.txTemplate = txTemplate;
    }

    private static final int PARALLELISM = 10;

    public void extractAndSave() {
        List<TrainingPlanEntity> trainingPlans = trainingPlanRepository.findAll();

        OutcomeCounter counter = new OutcomeCounter();
        AtomicInteger progress = new AtomicInteger(0);
        int total = trainingPlans.size();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Semaphore semaphore = new Semaphore(PARALLELISM);

            for (TrainingPlanEntity plan : trainingPlans) {
                semaphore.acquire();
                executor.submit(() -> {
                    try {
                        processStudentTrainings(plan, counter);
                        int done = progress.incrementAndGet();
                        if (done % 100 == 0) {
                            log.info("[STUDENT_TRAINING] Progress: {}/{}", done, total);
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
            log.error("[STUDENT_TRAINING] Interrupted during parallel extraction");
        }

        log.info("""
                [STUDENT_TRAINING_EXTRACTION]
                Source: Firestore
                Subcollection: {}
                Target: training_plan_training.student_exercises_json
                --------------------------------
                Updated:    {}
                Ignored:    {}
                Skipped:    {}
                API Errors: {}
                Total processed: {}
                """,
                props.getStudentTrainingSubcollection(),
                counter.getUpdated(),
                counter.getIgnored(),
                counter.getSkipped(),
                counter.getApiErrors(),
                counter.getSize()
        );
    }

    private void processStudentTrainings(TrainingPlanEntity plan, OutcomeCounter counter) {
        final String collection = props.getTrainingPlanCollection();
        final String subcollection = props.getStudentTrainingSubcollection();
        final String planRef = plan.getExternalRef();

        FirestoreSubCollectionResponse<List<FirestoreDocumentResponse<FreeTrainingFieldsResponse>>> doc;
        try {
            doc = firestore.listSubcollection(
                collection + "/" + planRef,
                subcollection,
                new ParameterizedTypeReference<>() {}
            );
        } catch (Exception e) {
            log.warn("[API_ERROR] Failed to fetch treinos/ subcollection for plan={}: {}",
                    planRef, e.getMessage());
            counter.increment(Outcome.API_ERROR);
            return;
        }

        if (doc == null || doc.documents() == null || doc.documents().isEmpty()) {
            log.debug("No student trainings returned for plan={}", planRef);
            return;
        }

        counter.setSize(doc.documents().size());

        for (FirestoreDocumentResponse<FreeTrainingFieldsResponse> docItem : doc.documents()) {
            var outcome = processStudentTraining(docItem, plan);
            counter.increment(outcome);
        }
    }

    private Outcome processStudentTraining(
            FirestoreDocumentResponse<FreeTrainingFieldsResponse> docItem,
            TrainingPlanEntity plan) {

        FreeTrainingFieldsResponse fields = docItem.fields();
        if (fields == null) {
            log.debug("Skipping: fields is null");
            return Outcome.SKIPPED;
        }

        String trainingRef = fields.refTreino() != null ? fields.refTreino().stringValue() : null;
        if (trainingRef == null || trainingRef.isBlank()) {
            trainingRef = extractDocId(docItem.name());
        }

        if (trainingRef == null || trainingRef.isBlank()) {
            log.debug("Skipping: no training ref found");
            return Outcome.SKIPPED;
        }

        String planRef = plan.getExternalRef();

        try {
            // Convert gruposExercicios to JSON string (not JsonNode — avoids OOM)
            String exercisesJsonStr = null;
            if (fields.gruposExercicios() != null) {
                exercisesJsonStr = objectMapper.writeValueAsString(fields.gruposExercicios());
            }

            String studentName = fields.nomeTreino() != null
                    ? fields.nomeTreino().stringValue().trim()
                    : null;

            final String tRef = trainingRef;
            final String eJson = exercisesJsonStr;
            final String sName = studentName;

            Integer rows = txTemplate.execute(status ->
                    trainingPlanTrainingRepository.updateStudentExercises(tRef, planRef, eJson, sName));

            if (rows == null || rows == 0) {
                log.debug("No TrainingPlanTraining found for trainingRef={} planRef={}, skipping",
                        trainingRef, planRef);
                return Outcome.SKIPPED;
            }

            log.debug("Updated student exercises for trainingRef={} planRef={}", trainingRef, planRef);
            return Outcome.UPDATED;

        } catch (Exception e) {
            log.error("Failed to update student exercises for trainingRef={} planRef={}: {}",
                    trainingRef, planRef, e.getMessage());
            return Outcome.SKIPPED;
        }
    }

    private String extractDocId(String documentPath) {
        if (documentPath == null) return null;
        int lastSlash = documentPath.lastIndexOf('/');
        return lastSlash >= 0 ? documentPath.substring(lastSlash + 1) : documentPath;
    }
}
