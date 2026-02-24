package com.carolai.extractor.service;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.carolai.extractor.config.ExternalApiProperties;

import reactor.core.publisher.Mono;

@Service
public class ExternalApiService {

    private static final Logger log = LogManager.getLogger(ExternalApiService.class);

    private final WebClient client;
    private final ExternalApiProperties props;

    public ExternalApiService(ExternalApiProperties props) {
        this.props = props;
        this.client = WebClient.builder()
                .baseUrl(props.getPhysicalAssessmentBaseUrl())
                .build();
    }

    public <T> T get(
            final String token,
            final String path,
            final Map<String, ?> queryParams,
            final Map<String, String> extraHeaders,
            final Class<T> responseType
    ) {

        WebClient.RequestHeadersSpec<?> spec = client.get()
                .uri(uriBuilder -> {
                    var b = uriBuilder.path(path);
                    if (queryParams != null) {
                        queryParams.forEach((k, v) -> {
                            if (v != null) b.queryParam(k, v);
                        });
                    }
                    return b.build();
                })
                .header("authorization", token)
                .header("accept", "*/*");

        if (extraHeaders != null && !extraHeaders.isEmpty()) {
            for (var e : extraHeaders.entrySet()) {
                if (e.getKey() != null && e.getValue() != null) {
                    spec = spec.header(e.getKey(), e.getValue());
                }
            }
        }

        return spec.retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> {
                                    final String msg = "Erro GET " + path +
                                            " status=" + resp.statusCode().value() +
                                            " body=" + body;
                                    log.error(msg);
                                    return Mono.error(new IllegalStateException(msg));
                                })
                )
                .bodyToMono(responseType)
                .block();
    }

    public String fetchPhysicalAssessment(final String refUsuario) {

        return get(props.getPhysicalAssessmentToken(),
                props.getPhysicalAssessmentEndpoint(),
                Map.of("refUsuario", refUsuario),
                null,
                String.class
        );
    }
}