package com.carolai.extractor.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.carolai.extractor.config.AuthApiProperties;
import com.carolai.extractor.dto.request.AuthRequest;
import com.carolai.extractor.dto.response.AuthResponse;

@Service
public class AuthService {

    private static final Logger log = LogManager.getLogger(AuthService.class);

    private final WebClient client;
    private final AuthApiProperties props;

    public AuthService(WebClient externalApiClient, AuthApiProperties props) {
        this.client = externalApiClient;
        this.props = props;
    }

    public String authenticate() {
        final AuthRequest req =
                new AuthRequest(props.getEmail(), props.getPassword(), true);

        final AuthResponse response = client.post()
                .uri(props.getUrl() + "?key=" + props.getKey())
                .bodyValue(req)
                .retrieve()
                .bodyToMono(AuthResponse.class)
                .block();

        if (response == null || response.idToken() == null) {
            throw new IllegalStateException("Auth retornou vazio (idToken null).");
        }

        return response.idToken();
    }
}