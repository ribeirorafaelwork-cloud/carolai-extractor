package com.carolai.extractor.outbox;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.carolai.extractor.outbox.dto.PopulateResult;
import com.carolai.extractor.outbox.mapper.ExerciseOutboxMapper;
import com.carolai.extractor.outbox.mapper.ObjectiveOutboxMapper;
import com.carolai.extractor.outbox.mapper.PhysicalAssessmentOutboxMapper;
import com.carolai.extractor.outbox.mapper.StudentOutboxMapper;
import com.carolai.extractor.outbox.mapper.TrainingHistoryOutboxMapper;
import com.carolai.extractor.persistence.entity.CustomerEntity;
import com.carolai.extractor.persistence.entity.CustomerObjectiveEntity;
import com.carolai.extractor.persistence.entity.ExportOutboxEntity;
import com.carolai.extractor.persistence.entity.PhysicalAssessmentEntity;
import com.carolai.extractor.persistence.entity.TrainingPlanEntity;
import com.carolai.extractor.persistence.repository.CustomerRepository;
import com.carolai.extractor.persistence.repository.ExerciseRepository;
import com.carolai.extractor.persistence.repository.ExportOutboxRepository;
import com.carolai.extractor.persistence.repository.PhysicalAssessmentRepository;

@ExtendWith(MockitoExtension.class)
class OutboxPopulationServiceTest {

    @Mock private CustomerRepository customerRepository;
    @Mock private PhysicalAssessmentRepository physicalAssessmentRepository;
    @Mock private ExerciseRepository exerciseRepository;
    @Mock private ExportOutboxRepository outboxRepository;
    @Mock private StudentOutboxMapper studentMapper;
    @Mock private TrainingHistoryOutboxMapper trainingHistoryMapper;
    @Mock private PhysicalAssessmentOutboxMapper assessmentMapper;
    @Mock private ObjectiveOutboxMapper objectiveMapper;
    @Mock private ExerciseOutboxMapper exerciseMapper;

    private OutboxPopulationService service;

    @BeforeEach
    void setUp() {
        service = new OutboxPopulationService(
                customerRepository,
                physicalAssessmentRepository,
                exerciseRepository,
                outboxRepository,
                studentMapper,
                trainingHistoryMapper,
                assessmentMapper,
                objectiveMapper,
                exerciseMapper
        );
    }

    // --- Student population tests ---

    @Test
    void populateStudents_insertsNewOutboxEntry() {
        CustomerEntity customer = customer("ext-1", "Maria", "maria@test.com");
        when(customerRepository.findAll()).thenReturn(List.of(customer));
        when(studentMapper.toCanonicalPayload(customer))
                .thenReturn(Map.of("fullName", "Maria", "email", "maria@test.com"));
        when(studentMapper.sourceKey(customer)).thenReturn("ext-1");
        when(outboxRepository.findByEntityTypeAndSourceKey("STUDENT", "ext-1"))
                .thenReturn(Optional.empty());
        when(outboxRepository.save(any(ExportOutboxEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PopulateResult result = service.populate("STUDENT");

        assertEquals(1, result.total());
        assertEquals(1, result.inserted());
        assertEquals(0, result.updated());
        assertEquals(0, result.unchanged());
        verify(outboxRepository).save(any(ExportOutboxEntity.class));
    }

    @Test
    void populateStudents_unchangedWhenHashMatches() {
        CustomerEntity customer = customer("ext-1", "Maria", "maria@test.com");
        Map<String, Object> payload = Map.of("fullName", "Maria", "email", "maria@test.com");

        when(customerRepository.findAll()).thenReturn(List.of(customer));
        when(studentMapper.toCanonicalPayload(customer)).thenReturn(payload);
        when(studentMapper.sourceKey(customer)).thenReturn("ext-1");

        // Simulate existing outbox entry with same hash
        String hash = com.carolai.extractor.migration.mapper.MigrationHashUtil.sha256(payload);
        ExportOutboxEntity existing = new ExportOutboxEntity();
        existing.setPayloadHash(hash);
        when(outboxRepository.findByEntityTypeAndSourceKey("STUDENT", "ext-1"))
                .thenReturn(Optional.of(existing));

        PopulateResult result = service.populate("STUDENT");

        assertEquals(1, result.total());
        assertEquals(0, result.inserted());
        assertEquals(0, result.updated());
        assertEquals(1, result.unchanged());
        verify(outboxRepository, never()).save(any());
    }

    @Test
    void populateStudents_updatesWhenHashChanges() {
        CustomerEntity customer = customer("ext-1", "Maria Silva", "maria@test.com");
        Map<String, Object> newPayload = Map.of("fullName", "Maria Silva", "email", "maria@test.com");

        when(customerRepository.findAll()).thenReturn(List.of(customer));
        when(studentMapper.toCanonicalPayload(customer)).thenReturn(newPayload);
        when(studentMapper.sourceKey(customer)).thenReturn("ext-1");

        ExportOutboxEntity existing = new ExportOutboxEntity();
        existing.setPayloadHash("old-hash-that-differs");
        existing.setStatus("ACKED");
        when(outboxRepository.findByEntityTypeAndSourceKey("STUDENT", "ext-1"))
                .thenReturn(Optional.of(existing));
        when(outboxRepository.save(any(ExportOutboxEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PopulateResult result = service.populate("STUDENT");

        assertEquals(1, result.total());
        assertEquals(0, result.inserted());
        assertEquals(1, result.updated());
        assertEquals(0, result.unchanged());

        ArgumentCaptor<ExportOutboxEntity> captor = ArgumentCaptor.forClass(ExportOutboxEntity.class);
        verify(outboxRepository).save(captor.capture());
        assertEquals("PENDING", captor.getValue().getStatus());
    }

    @Test
    void populateStudents_handlesMultipleCustomers() {
        CustomerEntity c1 = customer("ext-1", "Maria", "maria@test.com");
        CustomerEntity c2 = customer("ext-2", "Joao", "joao@test.com");

        when(customerRepository.findAll()).thenReturn(List.of(c1, c2));
        when(studentMapper.toCanonicalPayload(any())).thenReturn(Map.of("fullName", "Test"));
        when(studentMapper.sourceKey(c1)).thenReturn("ext-1");
        when(studentMapper.sourceKey(c2)).thenReturn("ext-2");
        when(outboxRepository.findByEntityTypeAndSourceKey(eq("STUDENT"), anyString()))
                .thenReturn(Optional.empty());
        when(outboxRepository.save(any(ExportOutboxEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PopulateResult result = service.populate("STUDENT");

        assertEquals(2, result.total());
        assertEquals(2, result.inserted());
        verify(outboxRepository, times(2)).save(any(ExportOutboxEntity.class));
    }

    // --- Objectives population tests ---

    @Test
    void populateObjectives_skipsCustomersWithNoObjectives() {
        CustomerEntity customer = customer("ext-1", "Maria", "maria@test.com");
        customer.setObjectives(List.of()); // empty objectives

        when(customerRepository.findAll()).thenReturn(List.of(customer));

        PopulateResult result = service.populate("OBJECTIVE");

        assertEquals(0, result.total());
        assertEquals(0, result.inserted());
        verify(outboxRepository, never()).save(any());
    }

    @Test
    void populateObjectives_insertsForCustomerWithObjectives() {
        CustomerEntity customer = customer("ext-1", "Maria", "maria@test.com");
        CustomerObjectiveEntity obj = new CustomerObjectiveEntity();
        obj.setObjectiveCode(1);
        obj.setName("Emagrecimento");
        obj.setSelected(true);
        customer.setObjectives(List.of(obj));

        when(customerRepository.findAll()).thenReturn(List.of(customer));
        when(objectiveMapper.toCanonicalPayload(customer))
                .thenReturn(Map.of("studentEmail", "maria@test.com", "objectives", List.of()));
        when(objectiveMapper.sourceKey(customer)).thenReturn("ext-1:objectives");
        when(outboxRepository.findByEntityTypeAndSourceKey("OBJECTIVE", "ext-1:objectives"))
                .thenReturn(Optional.empty());
        when(outboxRepository.save(any(ExportOutboxEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PopulateResult result = service.populate("OBJECTIVE");

        assertEquals(1, result.total());
        assertEquals(1, result.inserted());
    }

    // --- Training history population tests ---

    @Test
    void populateTrainingHistory_createsEntryPerPlan() {
        CustomerEntity customer = customer("ext-1", "Maria", "maria@test.com");
        TrainingPlanEntity plan1 = new TrainingPlanEntity();
        plan1.setExternalRef("plan-1");
        TrainingPlanEntity plan2 = new TrainingPlanEntity();
        plan2.setExternalRef("plan-2");
        customer.setTrainingPlans(List.of(plan1, plan2));

        Map<String, Object> payload1 = Map.of("planName", "Plan A");
        Map<String, Object> payload2 = Map.of("planName", "Plan B");

        when(customerRepository.findAll()).thenReturn(List.of(customer));
        when(trainingHistoryMapper.toCanonicalPayloads(customer)).thenReturn(List.of(payload1, payload2));
        when(trainingHistoryMapper.sourceKey(plan1)).thenReturn("ext-1:plan:plan-1");
        when(trainingHistoryMapper.sourceKey(plan2)).thenReturn("ext-1:plan:plan-2");
        when(outboxRepository.findByEntityTypeAndSourceKey(eq("TRAINING_HISTORY"), anyString()))
                .thenReturn(Optional.empty());
        when(outboxRepository.save(any(ExportOutboxEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PopulateResult result = service.populate("TRAINING_HISTORY");

        assertEquals(2, result.total());
        assertEquals(2, result.inserted());
        verify(outboxRepository, times(2)).save(any(ExportOutboxEntity.class));
    }

    // --- Physical assessments population tests ---

    @Test
    void populatePhysicalAssessments_insertsWhenCustomerFound() {
        CustomerEntity customer = customer("ext-1", "Maria", "maria@test.com");
        PhysicalAssessmentEntity assessment = new PhysicalAssessmentEntity();
        assessment.setCustomerId(1L);
        assessment.setDocumentKey("doc-1");

        when(physicalAssessmentRepository.findAll()).thenReturn(List.of(assessment));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(assessmentMapper.toCanonicalPayload(assessment, customer))
                .thenReturn(Map.of("studentEmail", "maria@test.com", "documentKey", "doc-1"));
        when(assessmentMapper.sourceKey(assessment)).thenReturn("1:assessment:doc-1");
        when(outboxRepository.findByEntityTypeAndSourceKey("PHYSICAL_ASSESSMENT", "1:assessment:doc-1"))
                .thenReturn(Optional.empty());
        when(outboxRepository.save(any(ExportOutboxEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PopulateResult result = service.populate("PHYSICAL_ASSESSMENT");

        assertEquals(1, result.total());
        assertEquals(1, result.inserted());
    }

    @Test
    void populatePhysicalAssessments_skipsWhenCustomerNotFound() {
        PhysicalAssessmentEntity assessment = new PhysicalAssessmentEntity();
        assessment.setCustomerId(999L);

        when(physicalAssessmentRepository.findAll()).thenReturn(List.of(assessment));
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        PopulateResult result = service.populate("PHYSICAL_ASSESSMENT");

        assertEquals(1, result.total());
        assertEquals(0, result.inserted());
        verify(outboxRepository, never()).save(any());
    }

    // --- populateAll tests ---

    @Test
    void populateAll_runsAllFiveEntityTypes() {
        when(customerRepository.findAll()).thenReturn(List.of());
        when(physicalAssessmentRepository.findAll()).thenReturn(List.of());
        when(exerciseRepository.findAll()).thenReturn(List.of());

        List<PopulateResult> results = service.populateAll();

        assertEquals(5, results.size());
        assertEquals("STUDENT", results.get(0).entityType());
        assertEquals("TRAINING_HISTORY", results.get(1).entityType());
        assertEquals("PHYSICAL_ASSESSMENT", results.get(2).entityType());
        assertEquals("OBJECTIVE", results.get(3).entityType());
        assertEquals("EXERCISE", results.get(4).entityType());
    }

    @Test
    void populate_throwsForUnknownType() {
        assertThrows(IllegalArgumentException.class, () -> service.populate("UNKNOWN"));
    }

    // --- Idempotency test ---

    @Test
    void populateStudents_idempotent_reRunProducesSameResult() {
        CustomerEntity customer = customer("ext-1", "Maria", "maria@test.com");
        Map<String, Object> payload = Map.of("fullName", "Maria", "email", "maria@test.com");
        String hash = com.carolai.extractor.migration.mapper.MigrationHashUtil.sha256(payload);

        when(customerRepository.findAll()).thenReturn(List.of(customer));
        when(studentMapper.toCanonicalPayload(customer)).thenReturn(payload);
        when(studentMapper.sourceKey(customer)).thenReturn("ext-1");

        // First run: entry doesn't exist
        when(outboxRepository.findByEntityTypeAndSourceKey("STUDENT", "ext-1"))
                .thenReturn(Optional.empty());
        when(outboxRepository.save(any(ExportOutboxEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PopulateResult first = service.populate("STUDENT");
        assertEquals(1, first.inserted());

        // Second run: entry exists with same hash
        ExportOutboxEntity existing = new ExportOutboxEntity();
        existing.setPayloadHash(hash);
        when(outboxRepository.findByEntityTypeAndSourceKey("STUDENT", "ext-1"))
                .thenReturn(Optional.of(existing));

        PopulateResult second = service.populate("STUDENT");
        assertEquals(0, second.inserted());
        assertEquals(1, second.unchanged());
    }

    // --- Helpers ---

    private CustomerEntity customer(String externalRef, String name, String email) {
        CustomerEntity c = new CustomerEntity();
        c.setId(1L);
        c.setExternalRef(externalRef);
        c.setName(name);
        c.setEmail(email);
        c.setTrainingPlans(List.of());
        c.setObjectives(List.of());
        return c;
    }
}
