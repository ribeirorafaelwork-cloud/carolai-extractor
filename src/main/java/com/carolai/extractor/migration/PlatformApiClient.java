package com.carolai.extractor.migration;

import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.carolai.extractor.config.PlatformApiProperties;
import com.carolai.extractor.migration.dto.CreateStudentPayload;
import com.carolai.extractor.migration.dto.PlatformLoginRequest;
import com.carolai.extractor.migration.dto.PlatformLoginResult;
import com.carolai.extractor.migration.dto.PlatformStudentResponse;
import com.carolai.extractor.migration.dto.SeedExercisesResponse;

@Component
public class PlatformApiClient {

    private static final Logger log = LogManager.getLogger(PlatformApiClient.class);
    private static final String API_PREFIX = "/api/v1";

    private final PlatformApiProperties props;
    private final WebClient webClient;
    private final AtomicReference<String> accessToken = new AtomicReference<>();

    public PlatformApiClient(PlatformApiProperties props) {
        this.props = props;
        this.webClient = WebClient.builder()
                .baseUrl(props.getUrl() != null ? props.getUrl() : "http://localhost:8081")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public void authenticate() {
        log.info("Authenticating with platform at {}", props.getUrl());

        var request = new PlatformLoginRequest(
                props.getEmail(),
                props.getPassword(),
                props.getTenantId()
        );

        PlatformLoginResult result = webClient.post()
                .uri(API_PREFIX + "/auth/login")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(PlatformLoginResult.class)
                .block();

        if (result == null || !result.authenticated() || result.tokens() == null) {
            throw new IllegalStateException("Platform authentication failed — check credentials and tenantId");
        }

        accessToken.set(result.tokens().accessToken());
        log.info("Authenticated with platform successfully");
    }

    public PlatformStudentResponse createStudent(CreateStudentPayload payload) {
        return executeWithRetry(() ->
            webClient.post()
                    .uri(API_PREFIX + "/students")
                    .headers(h -> applyAuth(h))
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(PlatformStudentResponse.class)
                    .block()
        );
    }

    public SeedExercisesResponse seedExercises() {
        return executeWithRetry(() ->
            webClient.post()
                    .uri(API_PREFIX + "/exercises/seed")
                    .headers(h -> applyAuth(h))
                    .retrieve()
                    .bodyToMono(SeedExercisesResponse.class)
                    .block()
        );
    }

    private void applyAuth(HttpHeaders headers) {
        String token = accessToken.get();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        if (props.getTenantId() != null) {
            headers.set("X-Tenant-Id", props.getTenantId());
        }
    }

    private <T> T executeWithRetry(java.util.function.Supplier<T> call) {
        try {
            return call.get();
        } catch (WebClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatusCode.valueOf(401)) {
                log.warn("Got 401, re-authenticating and retrying...");
                authenticate();
                return call.get();
            }
            throw ex;
        }
    }
}
