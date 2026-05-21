package com.tcc.pjb.backend.integration.judicial.financeiro;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.SisbajudHttpResponse;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "pjb.integrations.sisbajud", name = "mock-enabled", havingValue = "false", matchIfMissing = true)
class SisbajudHttpClientImpl implements SisbajudHttpClient {

    private static final Logger log = LoggerFactory.getLogger(SisbajudHttpClientImpl.class);

    private final String baseUrl;
    private final String token;
    private final Duration timeout;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    SisbajudHttpClientImpl(
            @Value("${pjb.integrations.sisbajud.base-url:}") String baseUrl,
            @Value("${pjb.integrations.sisbajud.token:}") String token,
            @Value("${pjb.integrations.sisbajud.timeout-seconds:30}") int timeoutSeconds,
            @Qualifier("pjbSharedHttpClient") HttpClient httpClient,
            ObjectMapper objectMapper) {
        this.baseUrl = baseUrl;
        this.token = token;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    @Override
    public SisbajudHttpResponse solicitarBloqueio(String cpfDevedor, BigDecimal valorSolicitado, String numeroOficio) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("cpfDevedor", cpfDevedor);
            body.put("valorSolicitado", valorSolicitado);
            body.put("numeroOficio", numeroOficio);
            String payload = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/bloqueios"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .timeout(timeout)
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("[SISBAJUD] bloqueio rejeitado status={} cpf={}", response.statusCode(),
                        Hashes.sha256HexPrefix(cpfDevedor, 8));
                throw new IllegalStateException("SISBAJUD retornou HTTP " + response.statusCode());
            }
            return objectMapper.readValue(response.body(), SisbajudHttpResponse.class);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[SISBAJUD] bloqueio falhou cpf={} err={}", Hashes.sha256HexPrefix(cpfDevedor, 8), e.getMessage());
            throw new IllegalStateException("SISBAJUD bloqueio falhou: " + e.getMessage(), e);
        }
    }
}
