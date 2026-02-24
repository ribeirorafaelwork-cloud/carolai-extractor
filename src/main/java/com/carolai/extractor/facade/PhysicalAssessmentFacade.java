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
        final List<CustomerEntity> customers = customerRepository.findAll();

        for (CustomerEntity customer : customers) {
            final String refUsuario = customer.getExternalRef();

            try {
                log.info("Buscando avaliação física para refUsuario={}", refUsuario);

                final String rawJson = externalApiService.fetchPhysicalAssessment(refUsuario);

                physicalAssessmentService.parseAndUpsertFromRawJson(customer, rawJson);
            } catch (Exception e) {
                log.error(
                        "Erro ao buscar avaliação física para refUsuario={}",
                        refUsuario,
                        e
                );
            }
        }
    }
}
