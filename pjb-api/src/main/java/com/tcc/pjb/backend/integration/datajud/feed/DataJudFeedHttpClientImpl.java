package com.tcc.pjb.backend.integration.datajud.feed;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "pjb.datajud.feed", name = "mock-enabled", havingValue = "false", matchIfMissing = true)
class DataJudFeedHttpClientImpl implements DataJudFeedHttpClient {

    private static final Logger log = LoggerFactory.getLogger(DataJudFeedHttpClientImpl.class);

    private final String baseUrl;
    private final String apiKey;
    private final Duration timeout;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    DataJudFeedHttpClientImpl(
            @Value("${pjb.datajud.feed.base-url:}") String baseUrl,
            @Value("${pjb.datajud.feed.api-key:}") String apiKey,
            @Value("${pjb.datajud.feed.timeout-seconds:30}") int timeoutSeconds,
            @Qualifier("pjbSharedHttpClient") HttpClient httpClient,
            ObjectMapper objectMapper) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    @Override
    public void bulkIndex(List<DataJudFeedEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        try {
            StringBuilder sb = new StringBuilder();
            for (DataJudFeedEntry entry : entries) {
                sb.append("{\"index\":{\"_id\":\"").append(entry.processoId()).append("\"}}\n");
                sb.append(objectMapper.writeValueAsString(entry)).append("\n");
            }
            String payload = sb.toString();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/_bulk"))
                    .header("Content-Type", "application/x-ndjson")
                    .header("Authorization", "ApiKey " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .timeout(timeout)
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("[DATAJUD] bulk index rejeitado status={} entries={}", response.statusCode(), entries.size());
                throw new IllegalStateException("DataJud retornou HTTP " + response.statusCode());
            }
            log.debug("[DATAJUD] bulk index ok entries={}", entries.size());
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[DATAJUD] bulk index falhou entries={} err={}", entries.size(), e.getMessage());
            throw new IllegalStateException("DataJud bulk index falhou: " + e.getMessage(), e);
        }
    }
}
