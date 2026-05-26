package com.tcc.pjb.backend.integration.serpro.datavalid;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SerproDataValidHttpClient implements CpfValidacaoPort {

    private static final Logger log = LoggerFactory.getLogger(SerproDataValidHttpClient.class);
    private static final Pattern CODIGO_SITUACAO = Pattern.compile("\"codigo\"\\s*:\\s*\"(\\d+)\"");
    private static final int CB_FAILURE_THRESHOLD = 3;
    private static final Duration CB_OPEN_DURATION = Duration.ofSeconds(30);

    private final SerproDataValidProperties properties;
    private final SerproDataValidOAuthTokenService tokenService;
    private final HttpClient httpClient;
    private final SimpleCircuitBreaker cb;

    SerproDataValidHttpClient(SerproDataValidProperties properties,
                              SerproDataValidOAuthTokenService tokenService,
                              HttpClient httpClient) {
        this.properties = Objects.requireNonNull(properties, "serproDataValidProperties");
        this.tokenService = Objects.requireNonNull(tokenService, "serproDataValidOAuthTokenService");
        this.httpClient = Objects.requireNonNull(httpClient, "pjbSharedHttpClient");
        this.cb = new SimpleCircuitBreaker(CB_FAILURE_THRESHOLD, CB_OPEN_DURATION);
    }

    @Override
    public CpfValidacaoResult consultar(String cpf) {
        if (!cb.allowRequest()) {
            throw new IllegalStateException("Circuito aberto para Serpro DataValid — tentativa bloqueada.");
        }
        try {
            String token = tokenService.getAccessToken();
            String url = properties.baseUrl().stripTrailing() + "/" + cpf.replaceAll("\\D", "");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/json")
                    .timeout(properties.readTimeout())
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 404) {
                cb.onSuccess();
                return new CpfValidacaoResult(CpfSituacao.NAO_ENCONTRADA);
            }
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Serpro DataValid retornou HTTP " + response.statusCode());
            }
            CpfSituacao situacao = parseSituacao(response.body());
            cb.onSuccess();
            return new CpfValidacaoResult(situacao);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            cb.onFailure();
            throw new IllegalStateException("Consulta Serpro DataValid interrompida.", ex);
        } catch (Exception ex) {
            cb.onFailure();
            log.warn("Falha na consulta Serpro DataValid: {}", ex.getMessage());
            throw new IllegalStateException("Falha na consulta Serpro DataValid: " + ex.getMessage(), ex);
        }
    }

    private static CpfSituacao parseSituacao(String body) {
        if (body == null || body.isBlank()) {
            return CpfSituacao.NAO_ENCONTRADA;
        }
        Matcher m = CODIGO_SITUACAO.matcher(body);
        if (!m.find()) {
            return CpfSituacao.NAO_ENCONTRADA;
        }
        return switch (m.group(1).trim()) {
            case "0" -> CpfSituacao.REGULAR;
            case "2" -> CpfSituacao.SUSPENSA;
            case "3" -> CpfSituacao.CANCELADA_POR_OBITO;
            case "4" -> CpfSituacao.PENDENTE_DE_REGULARIZACAO;
            case "5" -> CpfSituacao.CANCELADA_POR_MULTIPLICIDADE;
            case "8" -> CpfSituacao.NULA;
            default -> CpfSituacao.NAO_ENCONTRADA;
        };
    }

    static class SimpleCircuitBreaker {
        private final int failureThreshold;
        private final Duration openDuration;
        private int consecutiveFailures = 0;
        private Instant openUntil = null;

        SimpleCircuitBreaker(int failureThreshold, Duration openDuration) {
            this.failureThreshold = Math.max(1, failureThreshold);
            this.openDuration = openDuration == null ? Duration.ofSeconds(30) : openDuration;
        }

        synchronized boolean allowRequest() {
            if (openUntil == null) return true;
            if (Instant.now().isAfter(openUntil)) {
                openUntil = null;
                consecutiveFailures = 0;
                return true;
            }
            return false;
        }

        synchronized void onSuccess() {
            consecutiveFailures = 0;
            openUntil = null;
        }

        synchronized void onFailure() {
            consecutiveFailures++;
            if (consecutiveFailures >= failureThreshold) {
                openUntil = Instant.now().plus(openDuration);
            }
        }
    }
}
