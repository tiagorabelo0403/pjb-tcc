package com.tcc.pjb.backend.ai.legalai.dreaming.infra;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tcc.pjb.backend.ai.legalai.config.AnthropicProperties;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class AnthropicDreamingClientImpl implements AnthropicDreamingClient {

    private static final Logger log = LoggerFactory.getLogger(AnthropicDreamingClientImpl.class);

    private final RestClient restClient;
    private final AnthropicProperties properties;

    public AnthropicDreamingClientImpl(RestClient.Builder restClientBuilder, AnthropicProperties properties) {
        this.properties = properties;
        this.restClient = restClientBuilder
                .baseUrl(properties.baseUrl())
                .defaultHeader("x-api-key", properties.apiKey())
                .defaultHeader("anthropic-version", "2023-06-01")
                .defaultHeader("anthropic-beta",
                        properties.managedAgentsVersion() + ", " + properties.dreamingVersion())
                .build();
    }

    @Override
    @CircuitBreaker(name = "anthropic-dreaming", fallbackMethod = "criarDreamFallback")
    public AnthropicDreamRef criarDream(
            String inputStoreId,
            List<String> sessionIds,
            String instrucoes,
            String modelo) {
        Map<String, Object> body = new HashMap<>();
        body.put("type", "dreaming");
        body.put("model", modelo);
        body.put("input_memory_store_id", inputStoreId);
        if (sessionIds != null && !sessionIds.isEmpty()) {
            body.put("session_ids", sessionIds);
        }
        if (instrucoes != null && !instrucoes.isBlank()) {
            body.put("instructions", instrucoes);
        }

        AnthropicDreamApiResponse response = restClient.post()
                .uri("/v1/dreams")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(AnthropicDreamApiResponse.class);

        return new AnthropicDreamRef(response.id(), response.status());
    }

    public AnthropicDreamRef criarDreamFallback(
            String inputStoreId,
            List<String> sessionIds,
            String instrucoes,
            String modelo,
            Throwable t) {
        log.warn("Circuit breaker ativo para criarDream: {}", t.getMessage());
        throw new AnthropicApiUnavailableException("Dreaming API indisponível", t);
    }

    @Override
    @CircuitBreaker(name = "anthropic-dreaming", fallbackMethod = "consultarDreamFallback")
    public Optional<AnthropicDreamStatusRef> consultarDream(String anthropicDreamId) {
        AnthropicDreamApiResponse response = restClient.get()
                .uri("/v1/dreams/{id}", anthropicDreamId)
                .retrieve()
                .body(AnthropicDreamApiResponse.class);

        if (response == null) {
            return Optional.empty();
        }

        return Optional.of(new AnthropicDreamStatusRef(
                response.id(),
                response.status(),
                response.outputMemoryStoreId(),
                response.inputTokens(),
                response.outputTokens(),
                response.error()
        ));
    }

    public Optional<AnthropicDreamStatusRef> consultarDreamFallback(String anthropicDreamId, Throwable t) {
        log.warn("Circuit breaker ativo para consultarDream {}: {}", anthropicDreamId, t.getMessage());
        return Optional.empty();
    }

    @Override
    @CircuitBreaker(name = "anthropic-dreaming")
    public void cancelarDream(String anthropicDreamId) {
        restClient.post()
                .uri("/v1/dreams/{id}/cancel", anthropicDreamId)
                .retrieve()
                .toBodilessEntity();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AnthropicDreamApiResponse(
            String id,
            String status,
            @JsonProperty("output_memory_store_id") String outputMemoryStoreId,
            @JsonProperty("input_tokens") long inputTokens,
            @JsonProperty("output_tokens") long outputTokens,
            String error
    ) {}
}
