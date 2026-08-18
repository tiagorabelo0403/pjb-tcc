package com.tcc.pjb.backend.core.comunicacao.judicial;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class BnmpApiGateway {

    private static final Logger log = LoggerFactory.getLogger(BnmpApiGateway.class);
    private static final int TIMEOUT_SEG = 15;

    private final HttpClient httpClient;
    private final String baseUrl;
    private final String token;

    BnmpApiGateway(
            @Qualifier("hsmInterceptacaoHttpClient") HttpClient httpClient,
            @Value("${pjb.bnmp.base-url:https://bnmp.cnj.jus.br/api/v1}") String baseUrl,
            @Value("${pjb.bnmp.token:}") String token) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.baseUrl = baseUrl;
        this.token = token;
    }

    @CircuitBreaker(name = "bnmp-registro", fallbackMethod = "registrarFallback")
    String registrar(String payloadJson) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/mandados"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(payloadJson, StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(TIMEOUT_SEG))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                throw new IllegalStateException("BNMP retornou HTTP " + resp.statusCode());
            }
            return extrairNumeroBnmp(resp.body());
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("BNMP registrar falhou: " + e.getMessage(), e);
        }
    }

    String registrarFallback(String payloadJson, Throwable t) {
        log.warn("[BNMP] Circuit breaker ativo para registrar: {}", t.getMessage());
        throw new BnmpApiUnavailableException("BNMP registro indisponível", t);
    }

    void revogar(String numeroBnmp, String motivo) {
        try {
            String body = "{\"motivo\":\"" + escapeJson(motivo) + "\"}";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/mandados/" + numeroBnmp + "/revogar"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .PUT(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(TIMEOUT_SEG))
                    .build();
            httpClient.send(req, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            log.error("[BNMP] Erro ao revogar numeroBnmp={}. {}", numeroBnmp, e.getMessage());
        }
    }

    private static String extrairNumeroBnmp(String responseBody) {
        if (responseBody == null) {
            return "BNMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
        int idx = responseBody.indexOf("\"numeroBnmp\":");
        if (idx < 0) {
            return "BNMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
        int inicio = responseBody.indexOf('"', idx + 13) + 1;
        int fim = responseBody.indexOf('"', inicio);
        if (inicio > 0 && fim > inicio) {
            return responseBody.substring(inicio, fim);
        }
        throw new IllegalStateException("BNMP: campo numeroBnmp ausente ou malformado na resposta");
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ");
    }
}
