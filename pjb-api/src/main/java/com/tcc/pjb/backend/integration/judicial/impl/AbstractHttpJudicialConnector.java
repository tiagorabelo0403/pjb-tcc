package com.tcc.pjb.backend.integration.judicial.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.integration.judicial.ExternalProcessEvent;
import com.tcc.pjb.backend.integration.judicial.ExternalProcessSnapshot;
import com.tcc.pjb.backend.integration.judicial.JudicialIntegrationProperties;
import com.tcc.pjb.backend.integration.judicial.JudicialMapSupport;
import com.tcc.pjb.backend.integration.judicial.JudicialOAuthTokenService;
import com.tcc.pjb.backend.integration.judicial.JudicialProcessConnector;
import com.tcc.pjb.backend.integration.judicial.JudicialSubmissionCapability;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import com.tcc.pjb.backend.integration.judicial.ProtocolSubmissionRequest;
import com.tcc.pjb.backend.integration.judicial.ProtocolSubmissionResult;
import com.tcc.pjb.backend.integration.judicial.security.JudicialConnectorTransport;
import com.tcc.pjb.backend.integration.judicial.security.JudicialSecureHttpRequest;
import com.tcc.pjb.backend.integration.judicial.security.JudicialSecureHttpResponse;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

public abstract class AbstractHttpJudicialConnector implements JudicialProcessConnector {

    protected final JudicialIntegrationProperties props;
    protected final RestTemplate rest;
    protected final ObjectMapper objectMapper;
    protected final JudicialOAuthTokenService judicialOAuthTokenService;
    protected final JudicialConnectorTransport secureTransport;
    protected final Logger log;

    protected AbstractHttpJudicialConnector(JudicialIntegrationProperties props,
                                            RestTemplateBuilder builder,
                                            ObjectMapper objectMapper,
                                            JudicialOAuthTokenService judicialOAuthTokenService,
                                            Logger log) {
        this(props, builder, objectMapper, judicialOAuthTokenService, null, log);
    }

    protected AbstractHttpJudicialConnector(JudicialIntegrationProperties props,
                                            RestTemplateBuilder builder,
                                            ObjectMapper objectMapper,
                                            JudicialOAuthTokenService judicialOAuthTokenService,
                                            JudicialConnectorTransport secureTransport,
                                            Logger log) {
        this.props = props;
        this.rest = Objects.requireNonNull(builder).build();
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.judicialOAuthTokenService = judicialOAuthTokenService;
        this.secureTransport = secureTransport;
        this.log = Objects.requireNonNull(log);
    }

    protected abstract JudicialIntegrationProperties.Connector connectorConfig();

    protected abstract String connectorLabel();

    protected abstract List<String> acceptedDocumentTypes();

    protected abstract List<String> acceptedRamos();

    protected abstract List<String> acceptedScopes();

    protected List<String> snapshotPathCandidates() {
        return configuredPaths(
                connectorConfig() != null ? connectorConfig().getSnapshotPath() : null,
                List.of(
                        "/api/processos/{numero}",
                        "/processos/{numero}",
                        "/v1/processos/{numero}",
                        "/gateway/processos/{numero}",
                        "/api/v1/processos/{numero}"
                )
        );
    }

    protected List<String> eventsPathCandidates() {
        return configuredPaths(
                connectorConfig() != null ? connectorConfig().getEventsPath() : null,
                List.of(
                        "/api/processos/{numero}/eventos",
                        "/processos/{numero}/eventos",
                        "/v1/processos/{numero}/eventos",
                        "/gateway/processos/{numero}/eventos",
                        "/api/v1/processos/{numero}/eventos"
                )
        );
    }

    protected List<String> dryRunPathCandidates() {
        return configuredPaths(
                connectorConfig() != null ? connectorConfig().getDryRunPath() : null,
                List.of(
                        "/api/protocolos/preflight",
                        "/protocolos/preflight",
                        "/api/protocolos/dry-run",
                        "/protocolos/dry-run",
                        "/api/v1/protocolos/preflight"
                )
        );
    }

    protected List<String> submitPathCandidates() {
        return configuredPaths(
                connectorConfig() != null ? connectorConfig().getSubmitPath() : null,
                List.of(
                        "/api/protocolos",
                        "/protocolos",
                        "/gateway/protocolos",
                        "/api/v1/protocolos"
                )
        );
    }

    protected String baseUrl() {
        JudicialIntegrationProperties.Connector cfg = connectorConfig();
        if (cfg == null || cfg.getBaseUrl() == null || cfg.getBaseUrl().isBlank()) {
            return null;
        }
        String base = cfg.getBaseUrl().trim();
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    @Override
    public JudicialSubmissionCapability capability() {
        JudicialIntegrationProperties.Connector cfg = connectorConfig();
        return new JudicialSubmissionCapability(
                system(),
                cfg != null && cfg.isEnabled(),
                cfg != null && cfg.isEnabled(),
                cfg == null || cfg.isSupportsDryRun(),
                cfg == null || cfg.isSupportsSnapshotSync(),
                cfg == null || cfg.isSupportsEventSync(),
                cfg != null && cfg.isRequiresStepUpGovBr(),
                cfg != null && cfg.isRequiresCertificate(),
                cfg == null || cfg.isSupportsExternalMedia(),
                acceptedDocumentTypes(),
                acceptedRamos(),
                acceptedScopes(),
                baseUrl()
        );
    }

    @Override
    public Optional<ExternalProcessSnapshot> fetchSnapshotByNumero(String numeroUnificado) {
        String numero = normalizeNumero(numeroUnificado);
        if (numero == null) {
            return Optional.empty();
        }
        String base = baseUrl();
        if (base == null) {
            return Optional.empty();
        }
        Map<String, Object> lastFailure = Map.of();
        for (URI endpoint : buildGetEndpoints(base, snapshotPathCandidates(), numero, null)) {
            try {
                SimpleHttpResponse response = executeGet(endpoint, null, "SNAPSHOT_SYNC", JudicialMapSupport.compact("numeroUnificado", numero));
                Map<String, Object> payload = parseObjectPayload(response.body());
                if (!response.successful()) {
                    lastFailure = JudicialMapSupport.compact("endpoint", endpoint.toString(), "statusCode", response.statusCode(), "body", safeBody(response.body()));
                    if (response.clientError()) {
                        continue;
                    }
                    break;
                }
                if (payload.isEmpty()) {
                    lastFailure = JudicialMapSupport.compact("endpoint", endpoint.toString(), "statusCode", response.statusCode());
                    continue;
                }
                return Optional.of(buildSnapshot(numero, endpoint, response.statusCode(), payload));
            } catch (ResourceAccessException ex) {
                lastFailure = JudicialMapSupport.compact("endpoint", endpoint.toString(), "error", ex.getMessage());
            } catch (RestClientException ex) {
                lastFailure = JudicialMapSupport.compact("endpoint", endpoint.toString(), "error", ex.getMessage());
            }
        }
        if (!lastFailure.isEmpty()) {
            log.warn("[{}] Falha ao sincronizar snapshot para numero={} detalhes={}", connectorLabel(), numero, lastFailure);
        }
        return Optional.empty();
    }

    @Override
    public List<ExternalProcessEvent> fetchEvents(String numeroUnificado, Instant since) {
        String numero = normalizeNumero(numeroUnificado);
        if (numero == null) {
            return List.of();
        }
        String base = baseUrl();
        if (base == null) {
            return List.of();
        }
        for (URI endpoint : buildGetEndpoints(base, eventsPathCandidates(), numero, since)) {
            try {
                SimpleHttpResponse response = executeGet(endpoint, null, "EVENT_SYNC", JudicialMapSupport.compact("numeroUnificado", numero, "since", since != null ? since.toString() : null));
                if (!response.successful()) {
                    if (response.clientError()) {
                        continue;
                    }
                    log.warn("[{}] Falha ao sincronizar eventos numero={} endpoint={} status={} body={}", connectorLabel(), numero, endpoint, response.statusCode(), safeBody(response.body()));
                    continue;
                }
                Map<String, Object> payload = parseObjectPayload(response.body());
                List<Map<String, Object>> items = extractEventItems(payload);
                if (items.isEmpty()) {
                    continue;
                }
                List<ExternalProcessEvent> events = new ArrayList<>();
                int sequence = 0;
                for (Map<String, Object> item : items) {
                    sequence++;
                    events.add(buildEvent(numero, item, sequence));
                }
                return List.copyOf(events);
            } catch (ResourceAccessException ex) {
                log.warn("[{}] Falha de acesso ao sincronizar eventos numero={} endpoint={} erro={}", connectorLabel(), numero, endpoint, ex.getMessage());
            } catch (RestClientException ex) {
                log.warn("[{}] Falha ao sincronizar eventos numero={} endpoint={} erro={}", connectorLabel(), numero, endpoint, ex.getMessage());
            }
        }
        return List.of();
    }

    @Override
    public ProtocolSubmissionResult submit(ProtocolSubmissionRequest request) {
        if (request == null) {
            return new ProtocolSubmissionResult(false, system(), null, "INVALID_REQUEST", "Request de protocolo ausente.", Instant.now(), Map.of());
        }
        String base = baseUrl();
        if (base == null) {
            return new ProtocolSubmissionResult(false, system(), null, "NOT_CONFIGURED", "Base URL do " + connectorLabel() + " não configurada.", Instant.now(), JudicialMapSupport.compact("requestId", request.requestId()));
        }
        JudicialSubmissionCapability capability = capability();
        boolean dryRun = request.dryRun() && capability.supportsDryRun();
        List<URI> endpoints = buildProtocolEndpoints(base, request, dryRun);
        Map<String, Object> payload = buildSubmissionPayload(request, dryRun);
        ProtocolSubmissionResult lastFailure = null;
        for (URI endpoint : endpoints) {
            try {
                SimpleHttpResponse response = executePost(endpoint, payload, buildSubmissionHeaders(request, dryRun), dryRun ? "PROTOCOL_DRY_RUN" : "PROTOCOL_SUBMISSION", request.requestId(), mergeMetadata(request));
                Map<String, Object> raw = parseObjectPayload(response.body());
                ProtocolSubmissionResult result = buildSubmissionResult(request, endpoint, response.statusCode(), raw, dryRun);
                if (result.accepted()) {
                    return result;
                }
                lastFailure = result;
                if (response.clientError()) {
                    continue;
                }
            } catch (ResourceAccessException ex) {
                lastFailure = new ProtocolSubmissionResult(false, system(), null, "CONNECTOR_UNREACHABLE", ex.getMessage(), Instant.now(), JudicialMapSupport.compact("endpoint", endpoint.toString(), "requestId", request.requestId(), "dryRun", dryRun));
            } catch (RestClientException ex) {
                lastFailure = new ProtocolSubmissionResult(false, system(), null, "CONNECTOR_ERROR", ex.getMessage(), Instant.now(), JudicialMapSupport.compact("endpoint", endpoint.toString(), "requestId", request.requestId(), "dryRun", dryRun));
            }
        }
        if (lastFailure != null) {
            return lastFailure;
        }
        return new ProtocolSubmissionResult(false, system(), null, "NO_ENDPOINT", "Nenhum endpoint compatível foi resolvido para o conector " + connectorLabel() + '.', Instant.now(), JudicialMapSupport.compact("requestId", request.requestId(), "baseUrl", base, "dryRun", dryRun));
    }

    protected ExternalProcessSnapshot buildSnapshot(String numero,
                                                   URI endpoint,
                                                   Integer httpStatus,
                                                   Map<String, Object> payload) {
        String classe = firstText(payload, "classeProcessual", "classe", "classeTpu", "classe_tpu", "classeNome");
        String assunto = firstText(payload, "assunto", "assuntoPrincipal", "tema", "objeto", "titulo");
        NivelSigilo nivelSigilo = NivelSigilo.fromString(firstText(payload, "nivelSigilo", "sigilo", "grauSigilo", "nivel_sigilo"));
        return new ExternalProcessSnapshot(
                system(),
                firstNonBlank(firstText(payload, "numeroUnificado", "numeroProcesso", "numero", "nup"), numero),
                classe,
                assunto,
                nivelSigilo,
                Instant.now(),
                enrichRaw(payload, endpoint, httpStatus, null)
        );
    }

    protected ExternalProcessEvent buildEvent(String numero, Map<String, Object> payload, int sequence) {
        String externalId = firstText(payload, "id", "externalId", "codigo", "eventoId", "uuid");
        String type = firstNonBlank(firstText(payload, "tipo", "type", "descricaoTipo", "categoria"), "UNKNOWN");
        String description = firstNonBlank(firstText(payload, "descricao", "description", "titulo", "resumo"), type);
        Instant occurredAt = parseInstant(firstText(payload, "ocorridoEm", "occurredAt", "dataHora", "data", "timestamp"));
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
        String normalizedId = firstNonBlank(externalId, numero + ':' + sequence + ':' + occurredAt.toEpochMilli());
        return new ExternalProcessEvent(
                system(),
                numero,
                normalizedId,
                type,
                description,
                occurredAt,
                Map.copyOf(payload)
        );
    }

    protected Map<String, Object> buildSubmissionPayload(ProtocolSubmissionRequest request, boolean dryRun) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("requestId", request.requestId());
        payload.put("numeroUnificado", request.numeroUnificado());
        payload.put("title", request.title());
        payload.put("tribunalCodigo", request.tribunalCodigo());
        payload.put("unidadeJudiciariaCodigo", request.unidadeJudiciariaCodigo());
        payload.put("unidadeJudiciariaNome", request.unidadeJudiciariaNome());
        payload.put("rito", request.rito());
        payload.put("classeTpu", request.classeTpu());
        payload.put("ramoDireito", request.ramoDireito());
        payload.put("payloadJson", request.payloadJson());
        payload.put("integrityHash", request.integrityHash());
        payload.put("signerUserId", request.signerUserId());
        payload.put("executorUserId", request.executorUserId());
        payload.put("dryRun", dryRun);
        payload.put("metadata", request.metadata());
        payload.entrySet().removeIf(entry -> entry.getValue() == null);
        return Map.copyOf(payload);
    }

    protected ProtocolSubmissionResult buildSubmissionResult(ProtocolSubmissionRequest request,
                                                             URI endpoint,
                                                             Integer httpStatus,
                                                             Map<String, Object> raw,
                                                             boolean dryRun) {
        String status = firstNonBlank(firstText(raw, "status", "code", "resultado", "outcome"), httpStatus != null && httpStatus >= 200 && httpStatus < 300 ? (dryRun ? "DRY_RUN_OK" : "SUBMITTED") : "REJECTED");
        boolean accepted = httpStatus != null && httpStatus >= 200 && httpStatus < 300 && !isNegativeStatus(status);
        String protocolReference = extractProtocolReference(raw);
        if (protocolReference == null && dryRun) {
            protocolReference = dryRunReference(request);
        }
        String message = firstNonBlank(extractMessage(raw), accepted ? (dryRun ? "Pré-validação aceita para transmissão " + connectorLabel() + '.' : "Protocolo eletrônico aceito pelo conector " + connectorLabel() + '.') : "Conector judicial rejeitou a submissão.");
        return new ProtocolSubmissionResult(
                accepted,
                system(),
                protocolReference,
                normalizeStatus(status),
                message,
                Instant.now(),
                enrichRaw(raw, endpoint, request, httpStatus, dryRun)
        );
    }

    protected List<URI> buildGetEndpoints(String base,
                                          List<String> candidates,
                                          String numero,
                                          Instant since) {
        LinkedHashSet<URI> endpoints = new LinkedHashSet<>();
        for (String candidate : candidates) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(base).path(normalizePath(candidate));
            if (since != null) {
                builder.queryParam("since", since.toString());
            }
            endpoints.add(builder.buildAndExpand(Map.of("numero", numero)).encode(StandardCharsets.UTF_8).toUri());
        }
        return List.copyOf(endpoints);
    }

    protected List<URI> buildProtocolEndpoints(String base,
                                               ProtocolSubmissionRequest request,
                                               boolean dryRun) {
        LinkedHashSet<URI> endpoints = new LinkedHashSet<>();
        String override = request != null && request.metadata() != null
                ? firstNonBlank(text(request.metadata().get(dryRun ? "connectorDryRunPath" : "connectorProtocolPath")), text(request.metadata().get("connectorPath")))
                : null;
        if (override != null) {
            endpoints.add(UriComponentsBuilder.fromUriString(base).path(normalizePath(override)).build(true).toUri());
        }
        List<String> candidates = dryRun ? dryRunPathCandidates() : submitPathCandidates();
        for (String candidate : candidates) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(base).path(normalizePath(candidate));
            if (dryRun && candidate.contains("/protocolos") && !candidate.contains("preflight") && !candidate.contains("dry-run")) {
                builder.queryParam("dryRun", true);
            }
            endpoints.add(builder.build(true).toUri());
        }
        return List.copyOf(endpoints);
    }

    protected HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("User-Agent", "PJB-Judicial-Connector/2026.1");
        applyConnectorAuthentication(headers);
        applyStaticHeaders(headers);
        return headers;
    }

    protected HttpHeaders buildSubmissionHeaders(ProtocolSubmissionRequest request, boolean dryRun) {
        HttpHeaders headers = buildHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (request != null) {
            if (request.requestId() != null) {
                headers.set("X-PJB-Request-Id", request.requestId());
            }
            if (request.integrityHash() != null) {
                headers.set("X-PJB-Integrity-Hash", request.integrityHash());
            }
            if (request.tribunalCodigo() != null) {
                headers.set("X-PJB-Tribunal", request.tribunalCodigo());
            }
            if (request.unidadeJudiciariaCodigo() != null) {
                headers.set("X-PJB-Unidade", request.unidadeJudiciariaCodigo());
            }
            if (text(request.metadata().get("idempotencyKey")) != null) {
                headers.set("X-PJB-Idempotency-Key", text(request.metadata().get("idempotencyKey")));
            } else if (request.requestId() != null && !request.requestId().isBlank()) {
                headers.set("X-PJB-Idempotency-Key", request.requestId().trim());
            }
            if (text(request.metadata().get("signerMode")) != null) {
                headers.set("X-PJB-Signer-Mode", text(request.metadata().get("signerMode")));
            }
            if (text(request.metadata().get("retryPolicy")) != null) {
                headers.set("X-PJB-Retry-Policy", text(request.metadata().get("retryPolicy")));
            }
            applyRequestHeaderOverrides(headers, request.metadata());
        }
        if (connectorConfig() != null && connectorConfig().getCertificateAlias() != null && !connectorConfig().getCertificateAlias().isBlank()) {
            headers.set("X-PJB-Certificate-Alias", connectorConfig().getCertificateAlias().trim());
        }
        headers.set("X-PJB-Dry-Run", Boolean.toString(dryRun));
        return headers;
    }

    protected List<String> configuredPaths(String preferredPath, List<String> defaults) {
        LinkedHashSet<String> paths = new LinkedHashSet<>();
        if (preferredPath != null && !preferredPath.isBlank()) {
            paths.add(normalizePath(preferredPath));
        }
        if (defaults != null) {
            for (String candidate : defaults) {
                if (candidate != null && !candidate.isBlank()) {
                    paths.add(normalizePath(candidate));
                }
            }
        }
        return List.copyOf(paths);
    }

    protected String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String normalized = path.trim();
        return normalized.startsWith("/") ? normalized : '/' + normalized;
    }

    protected void applyConnectorAuthentication(HttpHeaders headers) {
        JudicialIntegrationProperties.Connector cfg = connectorConfig();
        if (headers == null || cfg == null) {
            return;
        }
        if (cfg.getBearerToken() != null && !cfg.getBearerToken().isBlank()) {
            headers.setBearerAuth(cfg.getBearerToken().trim());
            return;
        }
        if (judicialOAuthTokenService != null) {
            Optional<String> oauthToken = judicialOAuthTokenService.resolveAccessToken(system(), cfg);
            if (oauthToken.isPresent()) {
                headers.setBearerAuth(oauthToken.orElseThrow());
                return;
            }
        }
        if (cfg.getBasicUsername() != null && !cfg.getBasicUsername().isBlank() && cfg.getBasicPassword() != null && !cfg.getBasicPassword().isBlank()) {
            String raw = cfg.getBasicUsername().trim() + ':' + cfg.getBasicPassword();
            headers.set(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8)));
            return;
        }
        if (cfg.getApiKey() != null && !cfg.getApiKey().isBlank()) {
            headers.set(firstNonBlank(cfg.getApiKeyHeader(), "X-API-Key"), cfg.getApiKey().trim());
        }
    }

    protected void applyStaticHeaders(HttpHeaders headers) {
        JudicialIntegrationProperties.Connector cfg = connectorConfig();
        if (headers == null || cfg == null || cfg.getStaticHeaders() == null) {
            return;
        }
        cfg.getStaticHeaders().forEach((name, value) -> {
            if (name != null && !name.isBlank() && value != null && !value.isBlank()) {
                headers.set(name.trim(), value.trim());
            }
        });
    }

    protected void applyRequestHeaderOverrides(HttpHeaders headers, Map<String, Object> metadata) {
        if (headers == null || metadata == null || metadata.isEmpty()) {
            return;
        }
        Object rawHeaders = metadata.get("connectorHeaders");
        if (rawHeaders instanceof Map<?, ?> map) {
            map.forEach((key, value) -> {
                if (key != null && value != null) {
                    String name = String.valueOf(key).trim();
                    String headerValue = String.valueOf(value).trim();
                    if (!name.isBlank() && !headerValue.isBlank()) {
                        headers.set(name, headerValue);
                    }
                }
            });
        }
        Object apiKey = metadata.get("connectorApiKey");
        if (apiKey != null && !String.valueOf(apiKey).isBlank()) {
            String headerName = text(metadata.get("connectorApiKeyHeader"));
            headers.set(firstNonBlank(headerName, "X-API-Key"), String.valueOf(apiKey).trim());
        }
        Object authorization = metadata.get("connectorAuthorization");
        if (authorization != null && !String.valueOf(authorization).isBlank()) {
            headers.set(HttpHeaders.AUTHORIZATION, String.valueOf(authorization).trim());
        }
    }

    protected Map<String, Object> parseObjectPayload(String body) {
        if (body == null || body.isBlank()) {
            return Map.of();
        }
        String trimmed = body.trim();
        try {
            if (trimmed.startsWith("{")) {
                return JudicialMapSupport.copyNonNull(objectMapper.readValue(trimmed, new TypeReference<Map<String, Object>>() {
                }));
            }
            if (trimmed.startsWith("[")) {
                List<Object> items = objectMapper.readValue(trimmed, new TypeReference<List<Object>>() {
                });
                return JudicialMapSupport.compact("items", items);
            }
        } catch (Exception ignored) {
        }
        return JudicialMapSupport.compact("body", safeBody(trimmed));
    }

    protected List<Map<String, Object>> extractEventItems(Map<String, Object> payload) {
        Object raw = firstPresent(payload, "items", "eventos", "events", "data", "result", "payload");
        if (raw instanceof List<?> list) {
            List<Map<String, Object>> out = new ArrayList<>();
            for (Object item : list) {
                Map<String, Object> converted = asMap(item);
                if (!converted.isEmpty()) {
                    out.add(converted);
                }
            }
            return List.copyOf(out);
        }
        Map<String, Object> single = asMap(raw);
        if (!single.isEmpty()) {
            return List.of(single);
        }
        if (payload.containsKey("tipo") || payload.containsKey("type") || payload.containsKey("descricao")) {
            return List.of(payload);
        }
        return List.of();
    }

    protected Map<String, Object> asMap(Object source) {
        if (source == null) {
            return Map.of();
        }
        if (source instanceof Map<?, ?> map) {
            LinkedHashMap<String, Object> converted = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                String key = String.valueOf(entry.getKey()).trim();
                if (!key.isEmpty()) {
                    converted.put(key, entry.getValue());
                }
            }
            return converted.isEmpty() ? Map.of() : Map.copyOf(converted);
        }
        try {
            return JudicialMapSupport.copyNonNull(objectMapper.convertValue(source, new TypeReference<Map<String, Object>>() {
            }));
        } catch (IllegalArgumentException ignored) {
            return Map.of();
        }
    }

    protected Map<String, Object> enrichRaw(Map<String, Object> payload,
                                            URI endpoint,
                                            Integer httpStatus,
                                            Boolean dryRun) {
        LinkedHashMap<String, Object> raw = new LinkedHashMap<>();
        if (payload != null) {
            raw.putAll(payload);
        }
        raw.put("connector", connectorLabel());
        raw.put("system", system().name());
        raw.put("endpoint", endpoint != null ? endpoint.toString() : null);
        raw.put("httpStatus", httpStatus);
        raw.put("dryRun", dryRun);
        raw.put("fetchedAt", Instant.now().toString());
        raw.entrySet().removeIf(entry -> entry.getValue() == null);
        return Map.copyOf(raw);
    }

    protected Map<String, Object> enrichRaw(Map<String, Object> payload,
                                            URI endpoint,
                                            ProtocolSubmissionRequest request,
                                            Integer httpStatus,
                                            boolean dryRun) {
        LinkedHashMap<String, Object> raw = new LinkedHashMap<>();
        if (payload != null) {
            raw.putAll(payload);
        }
        raw.put("connector", connectorLabel());
        raw.put("system", system().name());
        raw.put("endpoint", endpoint != null ? endpoint.toString() : null);
        raw.put("httpStatus", httpStatus);
        raw.put("requestId", request != null ? request.requestId() : null);
        raw.put("tribunalCodigo", request != null ? request.tribunalCodigo() : null);
        raw.put("unidadeJudiciariaCodigo", request != null ? request.unidadeJudiciariaCodigo() : null);
        raw.put("dryRun", dryRun);
        raw.put("processedAt", Instant.now().toString());
        raw.entrySet().removeIf(entry -> entry.getValue() == null);
        return Map.copyOf(raw);
    }

    protected String extractProtocolReference(Map<String, Object> raw) {
        return firstText(raw, "protocolReference", "protocolo", "protocoloNumero", "id", "receipt", "recibo", "reference");
    }

    protected String extractMessage(Map<String, Object> raw) {
        return firstText(raw, "message", "mensagem", "detail", "descricao", "resultMessage");
    }

    protected String statusFromFailure(int httpStatus, Map<String, Object> raw) {
        String base = firstNonBlank(firstText(raw, "status", "code", "resultado", "outcome"), "HTTP_" + httpStatus);
        return normalizeStatus(base);
    }

    protected String normalizeStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return normalized.isBlank() ? null : normalized;
    }

    protected boolean isNegativeStatus(String status) {
        String normalized = normalizeStatus(status);
        if (normalized == null) {
            return false;
        }
        return normalized.contains("REJECT")
                || normalized.contains("DENY")
                || normalized.contains("BLOCK")
                || normalized.contains("INVALID")
                || normalized.contains("ERROR")
                || normalized.contains("FAIL");
    }

    protected String dryRunReference(ProtocolSubmissionRequest request) {
        int hash = request != null && request.integrityHash() != null ? request.integrityHash().hashCode() : 0;
        return "DRYRUN-" + system().name() + '-' + Integer.toHexString(hash).toUpperCase(Locale.ROOT);
    }

    protected String normalizeNumero(String numeroUnificado) {
        if (numeroUnificado == null) {
            return null;
        }
        String normalized = numeroUnificado.trim();
        return normalized.isBlank() ? null : normalized;
    }

    protected String firstText(Map<String, Object> map, String... keys) {
        Object value = firstPresent(map, keys);
        return text(value);
    }

    protected Object firstPresent(Map<String, Object> map, String... keys) {
        if (map == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (key == null || key.isBlank()) {
                continue;
            }
            Object value = map.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    protected String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String normalized = text(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    protected String text(Object value) {
        if (value == null) {
            return null;
        }
        String normalized = String.valueOf(value).trim();
        return normalized.isBlank() ? null : normalized;
    }

    protected Instant parseInstant(String raw) {
        String value = text(raw);
        if (value == null) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return OffsetDateTime.parse(value).toInstant();
        } catch (DateTimeParseException ignored) {
        }
        return null;
    }

    private Map<String, Object> mergeMetadata(ProtocolSubmissionRequest request) {
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        if (request != null && request.metadata() != null) {
            metadata.putAll(request.metadata());
        }
        if (request != null) {
            metadata.putIfAbsent("requestId", request.requestId());
            metadata.putIfAbsent("tribunalCodigo", request.tribunalCodigo());
            metadata.putIfAbsent("unidadeJudiciariaCodigo", request.unidadeJudiciariaCodigo());
        }
        metadata.entrySet().removeIf(entry -> entry.getValue() == null);
        return metadata.isEmpty() ? Map.of() : Map.copyOf(metadata);
    }

    protected String safeBody(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() <= 1600) {
            return normalized;
        }
        return normalized.substring(0, 1600);
    }

    private SimpleHttpResponse executeGet(URI endpoint,
                                          HttpHeaders headers,
                                          String operationName,
                                          Map<String, Object> metadata) {
        HttpHeaders effectiveHeaders = headers != null ? headers : buildHeaders();
        if (secureTransport != null) {
            JudicialSecureHttpResponse response = secureTransport.exchange(
                    system(),
                    metadata != null ? text(metadata.get("tribunalCodigo")) : null,
                    endpoint,
                    connectorConfig(),
                    JudicialSecureHttpRequest.get(toHeaderMap(effectiveHeaders), null, operationName, metadata != null ? text(metadata.get("requestId")) : null, metadata == null ? Map.of() : metadata)
            );
            return new SimpleHttpResponse(response.statusCode(), response.bodyAsString());
        }
        try {
            ResponseEntity<String> response = rest.exchange(endpoint, HttpMethod.GET, new HttpEntity<>(effectiveHeaders), String.class);
            return new SimpleHttpResponse(response.getStatusCode().value(), response.getBody());
        } catch (HttpStatusCodeException ex) {
            return new SimpleHttpResponse(ex.getStatusCode().value(), ex.getResponseBodyAsString());
        }
    }

    private SimpleHttpResponse executePost(URI endpoint,
                                           Map<String, Object> payload,
                                           HttpHeaders headers,
                                           String operationName,
                                           String correlationId,
                                           Map<String, Object> metadata) {
        if (secureTransport != null) {
            byte[] body;
            try {
                body = objectMapper.writeValueAsBytes(payload);
            } catch (Exception ex) {
                throw new RestClientException("Falha ao serializar payload do conector judicial.", ex);
            }
            LinkedHashMap<String, Object> effectiveMetadata = new LinkedHashMap<>();
            if (metadata != null) {
                effectiveMetadata.putAll(metadata);
            }
            effectiveMetadata.putIfAbsent("requestId", correlationId);
            JudicialSecureHttpResponse response = secureTransport.exchange(
                    system(),
                    metadata != null ? text(metadata.get("tribunalCodigo")) : null,
                    endpoint,
                    connectorConfig(),
                    JudicialSecureHttpRequest.post(body, toHeaderMap(headers), null, operationName, correlationId, Map.copyOf(effectiveMetadata))
            );
            return new SimpleHttpResponse(response.statusCode(), response.bodyAsString());
        }
        try {
            ResponseEntity<String> response = rest.exchange(endpoint, HttpMethod.POST, new HttpEntity<>(payload, headers), String.class);
            return new SimpleHttpResponse(response.getStatusCode().value(), response.getBody());
        } catch (HttpStatusCodeException ex) {
            return new SimpleHttpResponse(ex.getStatusCode().value(), ex.getResponseBodyAsString());
        }
    }

    private Map<String, List<String>> toHeaderMap(HttpHeaders headers) {
        if (headers == null || headers.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, List<String>> out = new LinkedHashMap<>();
        headers.forEach((name, values) -> {
            if (name != null && !name.isBlank() && values != null && !values.isEmpty()) {
                out.put(name, List.copyOf(values));
            }
        });
        return out.isEmpty() ? Map.of() : Map.copyOf(out);
    }

    private record SimpleHttpResponse(int statusCode, String body) {
        boolean successful() {
            return statusCode >= 200 && statusCode < 300;
        }

        boolean clientError() {
            return statusCode >= 400 && statusCode < 500;
        }
    }
}
