package com.carolai.extractor.facade;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import com.carolai.extractor.persistence.entity.CustomerEntity;
import com.carolai.extractor.persistence.repository.CustomerRepository;
import com.carolai.extractor.service.ExternalApiService;
import com.carolai.extractor.service.PhysicalAssessmentService;

@Component
public class PhysicalAssessmentFacade {

    private static final Logger log = LogManager.getLogger(PhysicalAssessmentFacade.class);

    private final ExternalApiService externalApiService;
    private final CustomerRepository customerRepository;
    private final PhysicalAssessmentService physicalAssessmentService;

    public PhysicalAssessmentFacade(
            ExternalApiService externalApiService,
            CustomerRepository customerRepository,
            PhysicalAssessmentService physicalAssessmentService
    ) {
        this.externalApiService = externalApiService;
        this.customerRepository = customerRepository;
        this.physicalAssessmentService = physicalAssessmentService;
    }

    public void extractAndSave() {
        if (!externalApiService.isPhysicalAssessmentConfigured()) {
            log.warn("⚠ [SKIP] physical-assessment-token não configurado — pulando extração de avaliações físicas");
            return;
        }

        final List<CustomerEntity> customers = customerRepository.findAll();
        int saved = 0, skipped = 0, apiErrors = 0, persistenceErrors = 0;

        for (CustomerEntity customer : customers) {
            final String refUsuario = customer.getExternalRef();

            String rawJson;
            try {
                rawJson = externalApiService.fetchPhysicalAssessment(refUsuario);
            } catch (Exception e) {
                apiErrors++;
                log.warn("⚠ [API_ERROR] Falha ao buscar avaliação física para customer={}|externalRef={}: {}",
                        customer.getId(), refUsuario, e.getMessage());
                continue;
            }

            try {
                int count = physicalAssessmentService.parseAndUpsertFromRawJson(customer, rawJson);
                saved += count;
                if (count == 0) skipped++;
            } catch (Exception e) {
                persistenceErrors++;
                log.error("❌ [PERSISTENCE_ERROR] Falha ao inserir/atualizar avaliação física para customer={}|externalRef={}: {}",
                        customer.getId(), refUsuario, e.getMessage(), e);
            }
        }

        log.info("""
                🎉 [PHYSICAL_ASSESSMENT_EXTRACTION]
                Source: External API
                Target: physical_assessment database
                --------------------------------
                Saved:              {}
                Skipped:            {}
                API Errors:         {}
                Persistence Errors: {}
                Total customers:    {}
                """,
                saved, skipped, apiErrors, persistenceErrors, customers.size());
    }
}
