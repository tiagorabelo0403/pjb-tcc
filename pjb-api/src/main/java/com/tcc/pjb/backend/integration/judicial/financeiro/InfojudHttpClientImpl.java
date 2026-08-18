package com.tcc.pjb.backend.integration.judicial.financeiro;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.InfojudConsultaResponse;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "pjb.integrations.infojud", name = "mock-enabled", havingValue = "false", matchIfMissing = true)
class InfojudHttpClientImpl implements InfojudHttpClient {

    private static final Logger log = LoggerFactory.getLogger(InfojudHttpClientImpl.class);

    private final String baseUrl;
    private final String token;
    private final Duration timeout;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    InfojudHttpClientImpl(
            @Value("${pjb.integrations.infojud.base-url:}") String baseUrl,
            @Value("${pjb.integrations.infojud.token:}") String token,
            @Value("${pjb.integrations.infojud.timeout-seconds:30}") int timeoutSeconds,
            @Qualifier("pjbSharedHttpClient") HttpClient httpClient,
            ObjectMapper objectMapper) {
        this.baseUrl = baseUrl;
        this.token = token;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    @Override
    public InfojudConsultaResponse consultar(String cpfCnpjConsultado) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of("cpfCnpjConsultado", cpfCnpjConsultado));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/consultas"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .timeout(timeout)
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("[INFOJUD] consulta rejeitada status={} cpf={}", response.statusCode(),
                        Hashes.sha256HexPrefix(cpfCnpjConsultado, 8));
                throw new IllegalStateException("INFOJUD retornou HTTP " + response.statusCode());
            }
            return objectMapper.readValue(response.body(), InfojudConsultaResponse.class);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[INFOJUD] consulta falhou cpf={} err={}", Hashes.sha256HexPrefix(cpfCnpjConsultado, 8), e.getMessage());
            throw new IllegalStateException("INFOJUD consulta falhou: " + e.getMessage(), e);
        }
    }
}
