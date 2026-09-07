package com.tcc.pjb.backend.ai.common.clients.ollama;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.ai.common.AiModelClient;

public class OllamaChatClient implements AiModelClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient http;
    private final String baseUrl;
    private final String model;
    private final double temperature;
    private volatile long timeoutMillis = 180_000;

    public OllamaChatClient(String baseUrl, String model, double temperature) {
        this.baseUrl = (baseUrl == null || baseUrl.isBlank()) ? "http://localhost:11434" : baseUrl;
        this.model = (model == null || model.isBlank()) ? "qwen2.5:7b" : model;
        this.temperature = Math.max(0.0, Math.min(2.0, temperature));
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    @Override
    public String generate(String prompt) {
        Objects.requireNonNull(prompt, "prompt");
        try {
            String url = trimSlash(baseUrl) + "/api/generate";

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", model);
            payload.put("prompt", prompt);
            payload.put("stream", false);
            payload.put("options", Map.of("temperature", temperature));

            String body = MAPPER.writeValueAsString(payload);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(timeoutMillis))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                return "[OLLAMA-ERROR] status=" + resp.statusCode() + " body=" + safeTrim(resp.body());
            }

            Map<String, Object> json = MAPPER.readValue(resp.body(), new TypeReference<>() {});
            Object response = json.get("response");
            if (response != null) {
                return String.valueOf(response);
            }

            return "[OLLAMA-EMPTY]";

        } catch (IOException e) {
            return "[OLLAMA-IO] " + e.getMessage();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "[OLLAMA-INTERRUPTED]";
        } catch (Exception e) {
            return "[OLLAMA-ERROR] " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    @Override
    public void setTimeout(long millis) {
        if (millis <= 0) return;
        this.timeoutMillis = millis;
    }

    private static String trimSlash(String s) {
        if (s == null) return "";
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    private static String safeTrim(String s) {
        if (s == null) return "";
        String t = s.replaceAll("[\r\n\t]+", " ").trim();
        return t.length() > 500 ? t.substring(0, 500) + "..." : t;
    }
}
