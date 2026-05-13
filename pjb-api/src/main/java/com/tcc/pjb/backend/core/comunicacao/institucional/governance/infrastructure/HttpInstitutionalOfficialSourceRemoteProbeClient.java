package com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialSourceConnectorRemoteProbeResult;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class HttpInstitutionalOfficialSourceRemoteProbeClient implements InstitutionalOfficialSourceRemoteProbeClient {

    private static final String DATAJUD_DEFAULT_BODY = "{\"size\":0,\"track_total_hits\":1,\"query\":{\"match_none\":{}}}";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final InstitutionalOfficialSourceConnectorProperties properties;

    public HttpInstitutionalOfficialSourceRemoteProbeClient(@Qualifier("pjbSharedHttpClient") HttpClient httpClient,
                                                            ObjectMapper objectMapper,
                                                            InstitutionalOfficialSourceConnectorProperties properties) {
        this.httpClient = Objects.requireNonNull(httpClient);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.properties = Objects.requireNonNull(properties);
    }

    @Override
    public InstitutionalOfficialSourceConnectorRemoteProbeResult probe(String sourceCode, URI target, Duration timeout) {
        Duration effectiveTimeout = timeout == null || timeout.isNegative() || timeout.isZero() ? Duration.ofSeconds(4) : timeout;
        return switch (normalizeSource(sourceCode)) {
            case "CNJ_DATAJUD" -> probeDataJud(sourceCode, target, effectiveTimeout);
            case "SIORG" -> probeJsonGet(sourceCode, target, effectiveTimeout, "siorg");
            case "IBGE_OU_TOPOLOGIA_CNJ" -> probeJsonGet(sourceCode, target, effectiveTimeout, "ibge");
            default -> probeGeneric(sourceCode, target, effectiveTimeout);
        };
    }

    private InstitutionalOfficialSourceConnectorRemoteProbeResult probeDataJud(String sourceCode, URI target, Duration timeout) {
        Instant start = Instant.now();
        LinkedHashSet<String> signals = new LinkedHashSet<>();
        LinkedHashSet<String> blockers = new LinkedHashSet<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        String apiKey = resolveApiKey(sourceCode);
        if (apiKey == null || apiKey.isBlank()) {
            blockers.add("datajud_api_key_missing");
            fundamentos.add("datajud_live_probe_requires_api_key=true");
            return new InstitutionalOfficialSourceConnectorRemoteProbeResult(false, -1, 0L, List.copyOf(signals), List.copyOf(blockers), List.copyOf(fundamentos));
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(target)
                    .timeout(timeout)
                    .header("User-Agent", "PJB/official-source-probe")
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("Authorization", "APIKey " + apiKey.trim())
                    .POST(HttpRequest.BodyPublishers.ofString(DATAJUD_DEFAULT_BODY))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long latency = Math.max(0L, Duration.between(start, Instant.now()).toMillis());
            signals.add("remote_probe_target=" + target);
            signals.add("remote_probe_http_status=" + response.statusCode());
            signals.add("remote_probe_latency_ms=" + latency);
            fundamentos.add("remote_probe_source=" + normalizeSource(sourceCode).toLowerCase(Locale.ROOT));
            fundamentos.add("remote_probe_http_status=" + response.statusCode());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode root = readJson(response.body());
                boolean timedOut = root.path("timed_out").asBoolean(false);
                int shardsFailed = root.path("_shards").path("failed").asInt(0);
                int hitsTotal = root.path("hits").path("total").path("value").asInt(0);
                signals.add("datajud_timed_out=" + timedOut);
                signals.add("datajud_shards_failed=" + shardsFailed);
                signals.add("datajud_hits_total=" + hitsTotal);
                fundamentos.add("datajud_match_none_probe=true");
                if (timedOut || shardsFailed > 0) {
                    blockers.add("datajud_unhealthy_payload");
                }
                return new InstitutionalOfficialSourceConnectorRemoteProbeResult(!timedOut && shardsFailed == 0, response.statusCode(), latency, List.copyOf(signals), List.copyOf(blockers), List.copyOf(fundamentos));
            }
            if (response.statusCode() == 401 || response.statusCode() == 403) {
                blockers.add("datajud_authorization_rejected");
            } else if (response.statusCode() >= 500 || response.statusCode() <= 0) {
                blockers.add("remote_probe_unhealthy_status=" + response.statusCode());
            }
            return new InstitutionalOfficialSourceConnectorRemoteProbeResult(response.statusCode() >= 200 && response.statusCode() < 500, response.statusCode(), latency, List.copyOf(signals), List.copyOf(blockers), List.copyOf(fundamentos));
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            blockers.add("remote_probe_exception=" + ex.getClass().getSimpleName().toLowerCase(Locale.ROOT));
            fundamentos.add("remote_probe_failure_for=" + normalizeSource(sourceCode).toLowerCase(Locale.ROOT));
            fundamentos.add("remote_probe_target=" + target);
            return new InstitutionalOfficialSourceConnectorRemoteProbeResult(false, -1, Math.max(0L, Duration.between(start, Instant.now()).toMillis()), List.copyOf(signals), List.copyOf(blockers), List.copyOf(fundamentos));
        }
    }

    private InstitutionalOfficialSourceConnectorRemoteProbeResult probeJsonGet(String sourceCode, URI target, Duration timeout, String family) {
        Instant start = Instant.now();
        LinkedHashSet<String> signals = new LinkedHashSet<>();
        LinkedHashSet<String> blockers = new LinkedHashSet<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        try {
            HttpRequest request = HttpRequest.newBuilder(target)
                    .timeout(timeout)
                    .header("User-Agent", "PJB/official-source-probe")
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long latency = Math.max(0L, Duration.between(start, Instant.now()).toMillis());
            signals.add("remote_probe_target=" + target);
            signals.add("remote_probe_http_status=" + response.statusCode());
            signals.add("remote_probe_latency_ms=" + latency);
            fundamentos.add("remote_probe_source=" + normalizeSource(sourceCode).toLowerCase(Locale.ROOT));
            fundamentos.add("remote_probe_http_status=" + response.statusCode());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode root = readJson(response.body());
                if ("ibge".equals(family)) {
                    signals.add("ibge_payload_kind=" + payloadKind(root));
                    if (root.isArray() && !root.isEmpty()) {
                        JsonNode first = root.get(0);
                        if (first.has("id")) {
                            signals.add("ibge_first_id=" + first.path("id").asText());
                        }
                        if (first.has("nome")) {
                            signals.add("ibge_first_nome=" + sanitizeValue(first.path("nome").asText()));
                        }
                    } else if (root.has("id")) {
                        signals.add("ibge_id=" + root.path("id").asText());
                        signals.add("ibge_nome=" + sanitizeValue(root.path("nome").asText()));
                    }
                } else if ("siorg".equals(family)) {
                    signals.add("siorg_payload_kind=" + payloadKind(root));
                    signals.add("siorg_non_empty=" + (!root.isMissingNode() && !root.isNull()));
                }
                return new InstitutionalOfficialSourceConnectorRemoteProbeResult(true, response.statusCode(), latency, List.copyOf(signals), List.copyOf(blockers), List.copyOf(fundamentos));
            }
            if (response.statusCode() >= 500 || response.statusCode() <= 0) {
                blockers.add("remote_probe_unhealthy_status=" + response.statusCode());
            }
            return new InstitutionalOfficialSourceConnectorRemoteProbeResult(response.statusCode() >= 200 && response.statusCode() < 500, response.statusCode(), latency, List.copyOf(signals), List.copyOf(blockers), List.copyOf(fundamentos));
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            blockers.add("remote_probe_exception=" + ex.getClass().getSimpleName().toLowerCase(Locale.ROOT));
            fundamentos.add("remote_probe_failure_for=" + normalizeSource(sourceCode).toLowerCase(Locale.ROOT));
            fundamentos.add("remote_probe_target=" + target);
            return new InstitutionalOfficialSourceConnectorRemoteProbeResult(false, -1, Math.max(0L, Duration.between(start, Instant.now()).toMillis()), List.copyOf(signals), List.copyOf(blockers), List.copyOf(fundamentos));
        }
    }

    private InstitutionalOfficialSourceConnectorRemoteProbeResult probeGeneric(String sourceCode, URI target, Duration timeout) {
        Instant start = Instant.now();
        LinkedHashSet<String> signals = new LinkedHashSet<>();
        LinkedHashSet<String> blockers = new LinkedHashSet<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        try {
            HttpResponse<Void> head = sendDiscarding(buildGenericRequest(target, timeout, "HEAD"));
            long latency = Math.max(0L, Duration.between(start, Instant.now()).toMillis());
            if (head.statusCode() == 405 || head.statusCode() == 501) {
                HttpResponse<Void> get = sendDiscarding(buildGenericRequest(target, timeout, "GET"));
                return genericResult(sourceCode, target, get.statusCode(), latency, signals, blockers, fundamentos);
            }
            return genericResult(sourceCode, target, head.statusCode(), latency, signals, blockers, fundamentos);
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            blockers.add("remote_probe_exception=" + ex.getClass().getSimpleName().toLowerCase(Locale.ROOT));
            fundamentos.add("remote_probe_failure_for=" + normalizeSource(sourceCode).toLowerCase(Locale.ROOT));
            fundamentos.add("remote_probe_target=" + target);
            return new InstitutionalOfficialSourceConnectorRemoteProbeResult(false, -1, Math.max(0L, Duration.between(start, Instant.now()).toMillis()), List.copyOf(signals), List.copyOf(blockers), List.copyOf(fundamentos));
        }
    }

    private HttpRequest buildGenericRequest(URI target, Duration timeout, String method) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(target)
                .timeout(timeout)
                .header("User-Agent", "PJB/official-source-probe")
                .header("Accept", "application/json, text/plain, */*");
        if ("HEAD".equals(method)) {
            builder.method("HEAD", HttpRequest.BodyPublishers.noBody());
        } else {
            builder.GET();
        }
        return builder.build();
    }

    private HttpResponse<Void> sendDiscarding(HttpRequest request) throws IOException, InterruptedException {
        return httpClient.send(request, HttpResponse.BodyHandlers.discarding());
    }

    private InstitutionalOfficialSourceConnectorRemoteProbeResult genericResult(String sourceCode,
                                                                                URI target,
                                                                                int statusCode,
                                                                                long latency,
                                                                                LinkedHashSet<String> signals,
                                                                                LinkedHashSet<String> blockers,
                                                                                LinkedHashSet<String> fundamentos) {
        boolean reachable = statusCode >= 200 && statusCode < 500;
        signals.add("remote_probe_target=" + target);
        signals.add("remote_probe_http_status=" + statusCode);
        signals.add("remote_probe_latency_ms=" + Math.max(0, latency));
        fundamentos.add("remote_probe_source=" + normalizeSource(sourceCode).toLowerCase(Locale.ROOT));
        fundamentos.add("remote_probe_http_status=" + statusCode);
        if (statusCode >= 500 || statusCode <= 0) {
            blockers.add("remote_probe_unhealthy_status=" + statusCode);
        }
        return new InstitutionalOfficialSourceConnectorRemoteProbeResult(reachable, statusCode, Math.max(0, latency), List.copyOf(signals), List.copyOf(blockers), List.copyOf(fundamentos));
    }

    private JsonNode readJson(String body) throws IOException {
        return objectMapper.readTree(body == null ? "{}" : body);
    }

    private String resolveApiKey(String sourceCode) {
        InstitutionalOfficialSourceConnectorProperties.SourceConfig config = properties.getSources().get(normalizeSource(sourceCode));
        return config == null ? null : config.getApiKey();
    }

    private static String payloadKind(JsonNode root) {
        if (root == null || root.isMissingNode() || root.isNull()) {
            return "empty";
        }
        if (root.isArray()) {
            return "array";
        }
        if (root.isObject()) {
            return "object";
        }
        return root.getNodeType().name().toLowerCase(Locale.ROOT);
    }

    private static String sanitizeValue(String value) {
        if (value == null || value.isBlank()) {
            return "indisponivel";
        }
        return value.trim().replace(' ', '_');
    }

    private static String normalizeSource(String sourceCode) {
        return sourceCode == null ? "desconhecida" : sourceCode.trim().toUpperCase(Locale.ROOT).replace('-', '_');
    }
}
