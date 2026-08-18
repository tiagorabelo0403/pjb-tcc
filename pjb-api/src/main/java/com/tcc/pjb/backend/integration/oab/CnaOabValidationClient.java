package com.tcc.pjb.backend.integration.oab;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.validation.oab.OabInfo;
import com.tcc.pjb.backend.model.entity.Usuario;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
public class CnaOabValidationClient implements OabValidationClient {

    private final OabValidationProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public CnaOabValidationClient(OabValidationProperties properties,
                                  ObjectMapper objectMapper,
                                  @Qualifier("pjbSharedHttpClient") HttpClient httpClient) {
        this.properties = Objects.requireNonNull(properties);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.httpClient = Objects.requireNonNull(httpClient);
    }

    @Override
    @CircuitBreaker(name = "oab-cna", fallbackMethod = "validateFallback")
    @Retry(name = "oab-cna")
    @Bulkhead(name = "oab-cna")
    public OabValidationResult validate(OabInfo info, Usuario usuario) {
        if (info == null) {
            return OabValidationResult.indeterminado("OAB_INFO_AUSENTE", "oab-cna");
        }
        if (!properties.enabled()) {
            return OabValidationResult.indeterminado("OAB_CNA_DESABILITADO", "oab-cna");
        }
        if (properties.baseUrl() == null) {
            return OabValidationResult.indeterminado("OAB_CNA_BASE_URL_AUSENTE", "oab-cna");
        }
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(buildUri(info))
                    .timeout(properties.requestTimeout())
                    .header(HttpHeaders.ACCEPT, "application/json")
                    .header(HttpHeaders.USER_AGENT, "PJB/1.0 oab-cna")
                    .GET();
            if (properties.apiKey() != null) {
                builder.header(properties.apiKeyHeader(), properties.apiKey());
            }
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return decode(response);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("oab_cna_interrupted", ex);
        } catch (Exception ex) {
            throw new IllegalStateException("oab_cna_unavailable", ex);
        }
    }

    public OabValidationResult validateFallback(OabInfo info, Usuario usuario, Throwable throwable) {
        return OabValidationResult.indeterminado("OAB_CNA_INDISPONIVEL", "oab-cna");
    }

    private URI buildUri(OabInfo info) {
        String base = removeTrailingSlash(properties.baseUrl());
        String path = properties.validationPath()
                .replace("{uf}", encode(info.uf()))
                .replace("{numero}", encode(info.numero()));
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        String uri = base + path;
        if (info.sufixo() != null && !info.sufixo().isBlank()) {
            uri = uri + (uri.contains("?") ? "&" : "?") + "sufixo=" + encode(info.sufixo());
        }
        return URI.create(uri);
    }

    private OabValidationResult decode(HttpResponse<String> response) throws Exception {
        int statusCode = response.statusCode();
        if (statusCode == 404) {
            return OabValidationResult.inapto("OAB_NAO_ENCONTRADA", "oab-cna");
        }
        if (statusCode < 200 || statusCode >= 300) {
            return OabValidationResult.indeterminado("OAB_CNA_HTTP_" + statusCode, "oab-cna");
        }
        String body = response.body();
        if (body == null || body.isBlank()) {
            return OabValidationResult.indeterminado("OAB_CNA_RESPOSTA_VAZIA", "oab-cna");
        }
        JsonNode root = objectMapper.readTree(body);
        Boolean regular = firstBoolean(root, "apto", "regular", "ativo", "habilitado");
        if (Boolean.TRUE.equals(regular)) {
            return OabValidationResult.apto("oab-cna");
        }
        if (Boolean.FALSE.equals(regular)) {
            return OabValidationResult.inapto("OAB_SITUACAO_INAPTA", "oab-cna");
        }
        String status = firstText(root, "status", "situacao", "situacaoInscricao", "regularidade", "resultado");
        return fromStatusText(status);
    }

    private OabValidationResult fromStatusText(String raw) {
        String value = normalize(raw);
        if (value == null) {
            return OabValidationResult.indeterminado("OAB_CNA_STATUS_AUSENTE", "oab-cna");
        }
        if (value.contains("APTO") || value.contains("REGULAR") || value.contains("ATIVO") || value.contains("ADIMPLENTE")) {
            return OabValidationResult.apto("oab-cna");
        }
        if (value.contains("INAPTO") || value.contains("SUSPENS") || value.contains("CANCEL") || value.contains("INATIVO") || value.contains("BAIXAD") || value.contains("LICENCIAD") || value.contains("IMPEDID")) {
            return OabValidationResult.inapto("OAB_SITUACAO_INAPTA", "oab-cna");
        }
        return OabValidationResult.indeterminado("OAB_CNA_STATUS_DESCONHECIDO", "oab-cna");
    }

    private static String firstText(JsonNode node, String... fieldNames) {
        if (node == null || node.isNull()) {
            return null;
        }
        for (String fieldName : fieldNames) {
            JsonNode value = node.findValue(fieldName);
            if (value != null && !value.isNull()) {
                String text = value.asText(null);
                if (text != null && !text.isBlank()) {
                    return text.trim();
                }
            }
        }
        return null;
    }

    private static Boolean firstBoolean(JsonNode node, String... fieldNames) {
        if (node == null || node.isNull()) {
            return null;
        }
        for (String fieldName : fieldNames) {
            JsonNode value = node.findValue(fieldName);
            if (value != null && value.isBoolean()) {
                return value.booleanValue();
            }
        }
        return null;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toUpperCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    private static String removeTrailingSlash(String value) {
        String normalized = value == null ? "" : value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
