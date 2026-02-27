package com.carolai.extractor.outbox;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carolai.extractor.migration.mapper.MigrationHashUtil;
import com.carolai.extractor.outbox.dto.PopulateResult;
import com.carolai.extractor.outbox.mapper.ExerciseOutboxMapper;
import com.carolai.extractor.outbox.mapper.ObjectiveOutboxMapper;
import com.carolai.extractor.outbox.mapper.PhysicalAssessmentOutboxMapper;
import com.carolai.extractor.outbox.mapper.StudentOutboxMapper;
import com.carolai.extractor.outbox.mapper.TrainingHistoryOutboxMapper;
import com.carolai.extractor.persistence.entity.CustomerEntity;
import com.carolai.extractor.persistence.entity.ExerciseEntity;
import com.carolai.extractor.persistence.entity.ExportOutboxEntity;
import com.carolai.extractor.persistence.entity.PhysicalAssessmentEntity;
import com.carolai.extractor.persistence.entity.TrainingPlanEntity;
import com.carolai.extractor.persistence.repository.CustomerRepository;
import com.carolai.extractor.persistence.repository.ExerciseRepository;
import com.carolai.extractor.persistence.repository.ExportOutboxRepository;
import com.carolai.extractor.persistence.repository.PhysicalAssessmentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class OutboxPopulationService {

    private static final Logger log = LogManager.getLogger(OutboxPopulationService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final CustomerRepository customerRepository;
    private final PhysicalAssessmentRepository physicalAssessmentRepository;
    private final ExerciseRepository exerciseRepository;
    private final ExportOutboxRepository outboxRepository;
    private final StudentOutboxMapper studentMapper;
    private final TrainingHistoryOutboxMapper trainingHistoryMapper;
    private final PhysicalAssessmentOutboxMapper assessmentMapper;
    private final ObjectiveOutboxMapper objectiveMapper;
    private final ExerciseOutboxMapper exerciseMapper;

    public OutboxPopulationService(
            CustomerRepository customerRepository,
            PhysicalAssessmentRepository physicalAssessmentRepository,
            ExerciseRepository exerciseRepository,
            ExportOutboxRepository outboxRepository,
            StudentOutboxMapper studentMapper,
            TrainingHistoryOutboxMapper trainingHistoryMapper,
            PhysicalAssessmentOutboxMapper assessmentMapper,
            ObjectiveOutboxMapper objectiveMapper,
            ExerciseOutboxMapper exerciseMapper
    ) {
        this.customerRepository = customerRepository;
        this.physicalAssessmentRepository = physicalAssessmentRepository;
        this.exerciseRepository = exerciseRepository;
        this.outboxRepository = outboxRepository;
        this.studentMapper = studentMapper;
        this.trainingHistoryMapper = trainingHistoryMapper;
        this.assessmentMapper = assessmentMapper;
        this.objectiveMapper = objectiveMapper;
        this.exerciseMapper = exerciseMapper;
    }

    @Transactional
    public List<PopulateResult> populateAll() {
        List<PopulateResult> results = new ArrayList<>();
        results.add(populateStudents());
        results.add(populateTrainingHistory());
        results.add(populatePhysicalAssessments());
        results.add(populateObjectives());
        results.add(populateExercises());
        return results;
    }

    @Transactional
    public PopulateResult populate(String entityType) {
        return switch (entityType.toUpperCase()) {
            case "STUDENT" -> populateStudents();
            case "TRAINING_HISTORY" -> populateTrainingHistory();
            case "PHYSICAL_ASSESSMENT" -> populatePhysicalAssessments();
            case "OBJECTIVE" -> populateObjectives();
            case "EXERCISE" -> populateExercises();
            default -> throw new IllegalArgumentException("Unknown entity type: " + entityType);
        };
    }

    private PopulateResult populateStudents() {
        List<CustomerEntity> customers = customerRepository.findAll();
        int inserted = 0, updated = 0, unchanged = 0;

        for (CustomerEntity customer : customers) {
            Map<String, Object> payload = studentMapper.toCanonicalPayload(customer);
            String sourceKey = studentMapper.sourceKey(customer);
            String hash = MigrationHashUtil.sha256(payload);

            UpsertOutcome outcome = upsertOutbox("STUDENT", sourceKey, payload, hash);
            switch (outcome) {
                case INSERTED -> inserted++;
                case UPDATED -> updated++;
                case UNCHANGED -> unchanged++;
            }
        }

        log.info("""
                🎉 [OUTBOX_POPULATE] STUDENT
                Source: customer database
                Target: export_outbox
                --------------------------------
                Inserted:  {}
                Updated:   {}
                Unchanged: {}
                Total processed: {}
                """,
                inserted, updated, unchanged, customers.size());
        return new PopulateResult("STUDENT", customers.size(), inserted, updated, unchanged);
    }

    private PopulateResult populateTrainingHistory() {
        List<CustomerEntity> customers = customerRepository.findAll();
        int total = 0, inserted = 0, updated = 0, unchanged = 0;

        for (CustomerEntity customer : customers) {
            List<Map<String, Object>> payloads = trainingHistoryMapper.toCanonicalPayloads(customer);

            for (int i = 0; i < payloads.size(); i++) {
                TrainingPlanEntity plan = customer.getTrainingPlans().get(i);
                Map<String, Object> payload = payloads.get(i);
                String sourceKey = trainingHistoryMapper.sourceKey(plan);
                String hash = MigrationHashUtil.sha256(payload);

                total++;
                UpsertOutcome outcome = upsertOutbox("TRAINING_HISTORY", sourceKey, payload, hash);
                switch (outcome) {
                    case INSERTED -> inserted++;
                    case UPDATED -> updated++;
                    case UNCHANGED -> unchanged++;
                }
            }
        }

        log.info("""
                🎉 [OUTBOX_POPULATE] TRAINING_HISTORY
                Source: customer + training_plan database
                Target: export_outbox
                --------------------------------
                Inserted:  {}
                Updated:   {}
                Unchanged: {}
                Total processed: {}
                """,
                inserted, updated, unchanged, total);
        return new PopulateResult("TRAINING_HISTORY", total, inserted, updated, unchanged);
    }

    private PopulateResult populatePhysicalAssessments() {
        List<PhysicalAssessmentEntity> assessments = physicalAssessmentRepository.findAll();
        int inserted = 0, updated = 0, unchanged = 0;

        // Build customer lookup for email resolution
        Map<Long, CustomerEntity> customerMap = new java.util.HashMap<>();
        Function<Long, CustomerEntity> customerLookup = customerId ->
                customerMap.computeIfAbsent(customerId, id ->
                        customerRepository.findById(id).orElse(null));

        for (PhysicalAssessmentEntity assessment : assessments) {
            CustomerEntity customer = customerLookup.apply(assessment.getCustomerId());
            if (customer == null) {
                log.warn("Customer not found for assessment customerId={}, skipping", assessment.getCustomerId());
                continue;
            }

            Map<String, Object> payload = assessmentMapper.toCanonicalPayload(assessment, customer);
            String sourceKey = assessmentMapper.sourceKey(assessment);
            String hash = MigrationHashUtil.sha256(payload);

            UpsertOutcome outcome = upsertOutbox("PHYSICAL_ASSESSMENT", sourceKey, payload, hash);
            switch (outcome) {
                case INSERTED -> inserted++;
                case UPDATED -> updated++;
                case UNCHANGED -> unchanged++;
            }
        }

        log.info("""
                🎉 [OUTBOX_POPULATE] PHYSICAL_ASSESSMENT
                Source: physical_assessment database
                Target: export_outbox
                --------------------------------
                Inserted:  {}
                Updated:   {}
                Unchanged: {}
                Total processed: {}
                """,
                inserted, updated, unchanged, assessments.size());
        return new PopulateResult("PHYSICAL_ASSESSMENT", assessments.size(), inserted, updated, unchanged);
    }

    private PopulateResult populateObjectives() {
        List<CustomerEntity> customers = customerRepository.findAll();
        int total = 0, inserted = 0, updated = 0, unchanged = 0;

        for (CustomerEntity customer : customers) {
            if (customer.getObjectives() == null || customer.getObjectives().isEmpty()) {
                continue;
            }

            total++;
            Map<String, Object> payload = objectiveMapper.toCanonicalPayload(customer);
            String sourceKey = objectiveMapper.sourceKey(customer);
            String hash = MigrationHashUtil.sha256(payload);

            UpsertOutcome outcome = upsertOutbox("OBJECTIVE", sourceKey, payload, hash);
            switch (outcome) {
                case INSERTED -> inserted++;
                case UPDATED -> updated++;
                case UNCHANGED -> unchanged++;
            }
        }

        log.info("""
                🎉 [OUTBOX_POPULATE] OBJECTIVE
                Source: customer database
                Target: export_outbox
                --------------------------------
                Inserted:  {}
                Updated:   {}
                Unchanged: {}
                Total processed: {}
                """,
                inserted, updated, unchanged, total);
        return new PopulateResult("OBJECTIVE", total, inserted, updated, unchanged);
    }

    private PopulateResult populateExercises() {
        List<ExerciseEntity> allExercises = exerciseRepository.findAll();

        // Deduplicate by normalized name (same normalization as sourceKey),
        // keep first display name and first non-null videoUrl
        Map<String, String> normalizedToDisplay = new LinkedHashMap<>();
        Map<String, String> normalizedToVideo = new LinkedHashMap<>();
        for (ExerciseEntity e : allExercises) {
            String name = e.getExerciseName();
            if (name == null || name.isBlank()) continue;
            String displayName = name.trim();
            String normalized = displayName.toLowerCase(java.util.Locale.ROOT).replace(" ", "_");

            if (!normalizedToDisplay.containsKey(normalized)) {
                normalizedToDisplay.put(normalized, displayName);
                normalizedToVideo.put(normalized, e.getVideoUrl());
            } else if (normalizedToVideo.get(normalized) == null && e.getVideoUrl() != null) {
                normalizedToVideo.put(normalized, e.getVideoUrl());
            }
        }

        int inserted = 0, updated = 0, unchanged = 0;
        for (String normalized : normalizedToDisplay.keySet()) {
            String displayName = normalizedToDisplay.get(normalized);
            String videoUrl = normalizedToVideo.get(normalized);
            Map<String, Object> payload = exerciseMapper.toCanonicalPayload(displayName, videoUrl);
            String sourceKey = exerciseMapper.sourceKey(displayName);
            String hash = MigrationHashUtil.sha256(payload);

            UpsertOutcome outcome = upsertOutbox("EXERCISE", sourceKey, payload, hash);
            switch (outcome) {
                case INSERTED -> inserted++;
                case UPDATED -> updated++;
                case UNCHANGED -> unchanged++;
            }
        }

        log.info("""
                🎉 [OUTBOX_POPULATE] EXERCISE
                Source: exercise database
                Target: export_outbox
                --------------------------------
                Inserted:  {}
                Updated:   {}
                Unchanged: {}
                Total processed: {}
                """,
                inserted, updated, unchanged, normalizedToDisplay.size());
        return new PopulateResult("EXERCISE", normalizedToDisplay.size(), inserted, updated, unchanged);
    }

    private UpsertOutcome upsertOutbox(String entityType, String sourceKey, Map<String, Object> payload, String hash) {
        JsonNode payloadNode = OBJECT_MAPPER.valueToTree(payload);

        Optional<ExportOutboxEntity> existing = outboxRepository.findByEntityTypeAndSourceKey(entityType, sourceKey);

        if (existing.isPresent()) {
            ExportOutboxEntity entity = existing.get();
            if (hash.equals(entity.getPayloadHash())) {
                return UpsertOutcome.UNCHANGED;
            }
            entity.setPayload(payloadNode);
            entity.setPayloadHash(hash);
            entity.setStatus("PENDING");
            entity.setAckedAt(null);
            entity.setErrorMsg(null);
            outboxRepository.save(entity);
            return UpsertOutcome.UPDATED;
        }

        ExportOutboxEntity entity = new ExportOutboxEntity();
        entity.setEntityType(entityType);
        entity.setSourceKey(sourceKey);
        entity.setPayload(payloadNode);
        entity.setPayloadHash(hash);
        entity.setStatus("PENDING");
        outboxRepository.save(entity);
        return UpsertOutcome.INSERTED;
    }

    private enum UpsertOutcome {
        INSERTED, UPDATED, UNCHANGED
    }
}
