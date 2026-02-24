package com.carolai.extractor.service;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import com.carolai.extractor.config.FirestoreProperties;
import com.carolai.extractor.dto.OutcomeCounter;
import com.carolai.extractor.dto.response.CustomerResponse;
import com.carolai.extractor.dto.response.FirestoreDocumentResponse;
import com.carolai.extractor.dto.response.RunQueryResponse;
import com.carolai.extractor.enums.Outcome;
import com.carolai.extractor.firestore.FirestoreQueryBuilder;
import com.carolai.extractor.persistence.entity.CustomerEntity;
import com.carolai.extractor.persistence.mapper.CustomerMapper;
import com.carolai.extractor.persistence.repository.CustomerRepository;

@Service
public class CustomerService {

    private static final Logger log = LogManager.getLogger(TrainingService.class);

    private final FirestoreClientService firestore;
    private final FirestoreProperties props;
    private final CustomerRepository customerRepo;
    private final CustomerMapper customerMapper;

    public CustomerService(FirestoreClientService firestore,
                           FirestoreProperties props,
                           CustomerRepository customerRepo,
                           CustomerMapper customerMapper) {
        this.firestore = firestore;
        this.props = props;
        this.customerRepo = customerRepo;
        this.customerMapper = customerMapper;
    }

    public void extractAndSave() {
        final String collection = props.getCustomerCollection();

        log.debug("📡 Starting Firestore customers extraction for personalId={} and collection={}",
                props.getPersonalId(), collection);

        var query = FirestoreQueryBuilder.create()
                .fromCollection(collection)
                .whereEquals("tipoPerfil", props.getTypeProfile())
                .whereEquals("idClienteApp", props.getIdClientApp());

        List<RunQueryResponse<FirestoreDocumentResponse<CustomerResponse>>> results = 
                firestore.runQuery(collection, query, new ParameterizedTypeReference<>() {});

        if (results == null || results.isEmpty()) {
            log.warn("⚠ No customers returned from Firestore.");
            return;
        }

        log.debug("📥 Firestore returned {} customers documents.", results.size());

        OutcomeCounter counter = new OutcomeCounter();
        counter.setSize(results.size());

        for (RunQueryResponse<FirestoreDocumentResponse<CustomerResponse>> r : results) {
            var outcome = processCustomer(r);

            counter.increment(outcome);
        }

        log.info("""
                🎉 [CUSTOMER_EXTRACTION]
                Source: Firestore
                Collection: {}
                Target: customer database
                --------------------------------
                Inserted: {}
                Updated:  {}
                Ignored:  {}
                Total processed: {}
                """,
                props.getCustomerCollection(),
                counter.getInserted(),
                counter.getUpdated(),
                counter.getIgnored(),
                counter.getSize()
        );

    }

    private Outcome processCustomer(RunQueryResponse<FirestoreDocumentResponse<CustomerResponse>> response) {

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
            CustomerEntity incoming = customerMapper.toEntity(fields);

            if (incoming.getExternalRef() == null) {
                log.debug("❌ Customer skipped — refTreino (getExternalRef) is NULL.");
                return Outcome.SKIPPED;
            }

            return saveCustomer(incoming);
        } catch (Exception e) {
            log.error("❌ Failed to process Customer document. {}", e.getMessage(), e);
            return Outcome.SKIPPED;
        }
    }

    private Outcome saveCustomer(CustomerEntity incoming) {

        CustomerEntity existing = customerRepo.findByExternalRef(incoming.getExternalRef()).orElse(null);

        if (existing == null) {
            customerRepo.save(incoming);
            log.debug("🆕 INSERTED: {} ({})", incoming.getName(), incoming.getId());
            return Outcome.INSERTED;
        }

        if (existing.getContentHash() != null &&
            existing.getContentHash().equals(incoming.getContentHash())) {

            log.debug("⏭️  IGNORED (no changes): {} ({})", incoming.getName(), incoming.getId());
            return Outcome.IGNORED;
        }

        incoming.setId(existing.getId());
        customerRepo.save(incoming);

        log.debug("🔄 UPDATED: {} ({})", incoming.getName(), incoming.getId());
        return Outcome.UPDATED;
    }
}
