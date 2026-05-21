package com.tcc.pjb.backend.integration.judicial.financeiro;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.RenajudRestricaoResponse;
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
@ConditionalOnProperty(prefix = "pjb.integrations.renajud", name = "mock-enabled", havingValue = "false", matchIfMissing = true)
class RenajudHttpClientImpl implements RenajudHttpClient {

    private static final Logger log = LoggerFactory.getLogger(RenajudHttpClientImpl.class);

    private final String baseUrl;
    private final String token;
    private final Duration timeout;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    RenajudHttpClientImpl(
            @Value("${pjb.integrations.renajud.base-url:}") String baseUrl,
            @Value("${pjb.integrations.renajud.token:}") String token,
            @Value("${pjb.integrations.renajud.timeout-seconds:30}") int timeoutSeconds,
            @Qualifier("pjbSharedHttpClient") HttpClient httpClient,
            ObjectMapper objectMapper) {
        this.baseUrl = baseUrl;
        this.token = token;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    @Override
    public RenajudRestricaoResponse solicitarRestricao(String placa, String renavam, String tipo) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("placa", placa);
            body.put("renavam", renavam);
            body.put("tipo", tipo);
            String payload = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/restricoes"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .timeout(timeout)
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("[RENAJUD] restricao rejeitada status={} placa={}", response.statusCode(), placa);
                throw new IllegalStateException("RENAJUD retornou HTTP " + response.statusCode());
            }
            return objectMapper.readValue(response.body(), RenajudRestricaoResponse.class);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[RENAJUD] restricao falhou placa={} err={}", placa, e.getMessage());
            throw new IllegalStateException("RENAJUD restricao falhou: " + e.getMessage(), e);
        }
    }
}
