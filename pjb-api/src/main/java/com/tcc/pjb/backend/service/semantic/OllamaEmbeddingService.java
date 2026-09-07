package com.tcc.pjb.backend.service.semantic;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "pjb.ai.embedding", name = "mode", havingValue = "ollama")
public class OllamaEmbeddingService implements EmbeddingService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient http;
    private final String baseUrl;
    private final String model;

    public OllamaEmbeddingService(@Value("${pjb.ai.ollama.base-url:http://localhost:11434}") String baseUrl,
                                  @Value("${pjb.ai.ollama.embedding-model:nomic-embed-text}") String model) {
        this.baseUrl = (baseUrl == null || baseUrl.isBlank()) ? "http://localhost:11434" : baseUrl;
        this.model = (model == null || model.isBlank()) ? "nomic-embed-text" : model;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    @Override
    public EmbeddingVector embed(String text) {
        String safeText = text == null ? "" : text;
        try {
            String url = trimSlash(baseUrl) + "/api/embed";

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", model);
            payload.put("input", safeText);

            String body = MAPPER.writeValueAsString(payload);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                throw new OllamaEmbeddingException("status=" + resp.statusCode() + " body=" + safeTrim(resp.body()));
            }

            Map<String, Object> json = MAPPER.readValue(resp.body(), new TypeReference<>() {});
            Object embeddingsObj = json.get("embeddings");
            if (embeddingsObj instanceof List<?> embeddings && !embeddings.isEmpty()
                    && embeddings.get(0) instanceof List<?> firstVector) {
                float[] values = new float[firstVector.size()];
                for (int i = 0; i < firstVector.size(); i++) {
                    values[i] = ((Number) firstVector.get(i)).floatValue();
                }
                return new EmbeddingVector(values).normalized();
            }

            throw new OllamaEmbeddingException("resposta sem campo embeddings");

        } catch (IOException e) {
            throw new OllamaEmbeddingException(e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OllamaEmbeddingException("interrompido", e);
        }
    }

    private static String trimSlash(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    private static String safeTrim(String s) {
        if (s == null) return "";
        String t = s.replaceAll("[\r\n\t]+", " ").trim();
        return t.length() > 500 ? t.substring(0, 500) + "..." : t;
    }

    public static final class OllamaEmbeddingException extends RuntimeException {
        public OllamaEmbeddingException(String message) {
            super(message);
        }

        public OllamaEmbeddingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
