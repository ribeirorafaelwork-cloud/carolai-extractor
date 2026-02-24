package com.carolai.extractor.service;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.carolai.extractor.config.FirestoreProperties;
import com.carolai.extractor.dto.response.FirestoreDocumentResponse;
import com.carolai.extractor.dto.response.FirestoreSubCollectionResponse;
import com.carolai.extractor.dto.response.RunQueryResponse;
import com.carolai.extractor.firestore.FirestoreQueryBuilder;

@Service
public class FirestoreClientService {

    private static final Logger log = LogManager.getLogger(FirestoreClientService.class);

    private final WebClient webClient;
    private final FirestoreProperties props;

    public FirestoreClientService(FirestoreProperties props) {
        this.props = props;

        this.webClient = WebClient.builder()
                .codecs(cfg -> cfg.defaultCodecs().maxInMemorySize(50 * 1024 * 1024))
                .build();
    }

    public <T> List<RunQueryResponse<T>> runQuery(String collection, FirestoreQueryBuilder builder, ParameterizedTypeReference<List<RunQueryResponse<T>>> typeRef) {
        String url = props.getBaseUrl() + ":runQuery";
        String jsonBody = builder
                .fromCollection(collection)
                .toJson();

        log.debug("📨 Executando consulta Firestore na coleção '{}': {}", collection, jsonBody);

        return webClient.post()
                .uri(url)
                .bodyValue(jsonBody)
                .retrieve()
                .bodyToMono(typeRef)
                .block();
    }

    public <T> FirestoreSubCollectionResponse<List<FirestoreDocumentResponse<T>>> listSubcollection(
            String parentDocumentPath,
            String subcollection,
            ParameterizedTypeReference<FirestoreSubCollectionResponse<List<FirestoreDocumentResponse<T>>>> typeRef
    ) {
        String url = props.getBaseUrl()
                + "/"
                + parentDocumentPath
                + "/"
                + subcollection;

        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(typeRef)
                .block();
    }

}