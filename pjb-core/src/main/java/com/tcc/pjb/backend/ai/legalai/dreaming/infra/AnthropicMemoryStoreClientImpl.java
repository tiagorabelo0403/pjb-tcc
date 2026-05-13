package com.tcc.pjb.backend.ai.legalai.dreaming.infra;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tcc.pjb.backend.ai.legalai.config.AnthropicProperties;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryAccessPolicy;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Optional;

@Component
public class AnthropicMemoryStoreClientImpl implements AnthropicMemoryStoreClient {

    private static final Logger log = LoggerFactory.getLogger(AnthropicMemoryStoreClientImpl.class);

    private final RestClient restClient;
    private final AnthropicProperties properties;

    public AnthropicMemoryStoreClientImpl(RestClient.Builder restClientBuilder, AnthropicProperties properties) {
        this.properties = properties;
        this.restClient = restClientBuilder
                .baseUrl(properties.baseUrl())
                .defaultHeader("x-api-key", properties.apiKey())
                .defaultHeader("anthropic-version", "2023-06-01")
                .defaultHeader("anthropic-beta", properties.managedAgentsVersion())
                .build();
    }

    @Override
    @CircuitBreaker(name = "anthropic-memory", fallbackMethod = "criarStoreFallback")
    public AnthropicStoreRef criarStore(String nome, String descricao, MemoryAccessPolicy.AccessType access) {
        Map<String, Object> body = Map.of(
                "name", nome,
                "description", descricao != null ? descricao : "",
                "access", access.name().toLowerCase()
        );
        AnthropicStoreApiResponse response = restClient.post()
                .uri("/v1/memory_stores")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(AnthropicStoreApiResponse.class);
        return new AnthropicStoreRef(response.id(), response.name(), response.status());
    }

    public AnthropicStoreRef criarStoreFallback(String nome, String descricao, MemoryAccessPolicy.AccessType access, Throwable t) {
        log.warn("Circuit breaker ativo para criarStore: {}", t.getMessage());
        throw new AnthropicApiUnavailableException("Memory Store API indisponível", t);
    }

    @Override
    @CircuitBreaker(name = "anthropic-memory", fallbackMethod = "buscarStoreFallback")
    public Optional<AnthropicStoreRef> buscarStore(String anthropicStoreId) {
        try {
            AnthropicStoreApiResponse response = restClient.get()
                    .uri("/v1/memory_stores/{id}", anthropicStoreId)
                    .retrieve()
                    .body(AnthropicStoreApiResponse.class);
            return Optional.of(new AnthropicStoreRef(response.id(), response.name(), response.status()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<AnthropicStoreRef> buscarStoreFallback(String anthropicStoreId, Throwable t) {
        log.warn("Circuit breaker ativo para buscarStore: {}", t.getMessage());
        return Optional.empty();
    }

    @Override
    @CircuitBreaker(name = "anthropic-memory")
    public void arquivarStore(String anthropicStoreId) {
        restClient.post()
                .uri("/v1/memory_stores/{id}/archive", anthropicStoreId)
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    @CircuitBreaker(name = "anthropic-memory", fallbackMethod = "criarMemoryFallback")
    public AnthropicMemoryEntryRef criarMemory(String anthropicStoreId, String chave, String conteudo) {
        Map<String, Object> body = Map.of("key", chave, "content", conteudo);
        AnthropicMemoryApiResponse response = restClient.post()
                .uri("/v1/memory_stores/{storeId}/memories", anthropicStoreId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(AnthropicMemoryApiResponse.class);
        return new AnthropicMemoryEntryRef(response.id(), response.key(), response.content(), response.version());
    }

    public AnthropicMemoryEntryRef criarMemoryFallback(String storeId, String chave, String conteudo, Throwable t) {
        log.warn("Circuit breaker ativo para criarMemory: {}", t.getMessage());
        throw new AnthropicApiUnavailableException("Memory API indisponível", t);
    }

    @Override
    @CircuitBreaker(name = "anthropic-memory")
    public AnthropicMemoryEntryRef atualizarMemory(String anthropicStoreId, String memoryId, String conteudo) {
        Map<String, Object> body = Map.of("content", conteudo);
        AnthropicMemoryApiResponse response = restClient.put()
                .uri("/v1/memory_stores/{storeId}/memories/{memId}", anthropicStoreId, memoryId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(AnthropicMemoryApiResponse.class);
        return new AnthropicMemoryEntryRef(response.id(), response.key(), response.content(), response.version());
    }

    @Override
    @CircuitBreaker(name = "anthropic-memory")
    public void deletarMemory(String anthropicStoreId, String memoryId) {
        restClient.delete()
                .uri("/v1/memory_stores/{storeId}/memories/{memId}", anthropicStoreId, memoryId)
                .retrieve()
                .toBodilessEntity();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AnthropicStoreApiResponse(String id, String name, String status) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AnthropicMemoryApiResponse(
            String id,
            String key,
            String content,
            @JsonProperty("memory_version") int version
    ) {}
}
