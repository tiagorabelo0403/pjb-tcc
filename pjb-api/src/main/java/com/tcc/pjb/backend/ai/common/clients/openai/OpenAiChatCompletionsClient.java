package com.tcc.pjb.backend.ai.common.clients.openai;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.ai.common.AiModelClient;

public class OpenAiChatCompletionsClient implements AiModelClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient http;
    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final double temperature;
    private final int maxTokens;
    private final String versionTag;
    private volatile long timeoutMillis = 45_000;

    public OpenAiChatCompletionsClient(
            String apiKey,
            String baseUrl,
            String model,
            double temperature,
            int maxTokens,
            String versionTag
    ) {
        this.apiKey = Objects.requireNonNull(apiKey, "apiKey");
        this.baseUrl = (baseUrl == null || baseUrl.isBlank()) ? "https://api.openai.com/v1" : baseUrl;
        this.model = (model == null || model.isBlank()) ? "gpt-5.2-thinking" : model;
        this.temperature = Math.max(0.0, Math.min(2.0, temperature));
        this.maxTokens = Math.max(64, maxTokens);
        this.versionTag = versionTag == null ? "v?" : versionTag;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    @Override
    public String generate(String prompt) {
        try {
            String url = trimSlash(baseUrl) + "/chat/completions";

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", model);
            payload.put("temperature", temperature);
            payload.put("max_tokens", maxTokens);
            payload.put("messages", List.of(
                    Map.of("role", "system", "content", "Você é o assistente do PJB (" + versionTag + "). Responda com precisão, sem inventar fatos. Quando solicitado, responda em JSON válido."),
                    Map.of("role", "user", "content", prompt)
            ));

            String body = MAPPER.writeValueAsString(payload);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(timeoutMillis))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                return "[OPENAI-ERROR] status=" + resp.statusCode() + " body=" + safeTrim(resp.body());
            }

            Map<String, Object> json = MAPPER.readValue(resp.body(), new TypeReference<>() {});
            Object choicesObj = json.get("choices");
            if (choicesObj instanceof List<?> choices && !choices.isEmpty()) {
                Object c0 = choices.get(0);
                if (c0 instanceof Map<?, ?> m) {
                    Object msg = m.get("message");
                    if (msg instanceof Map<?, ?> mm) {
                        Object content = mm.get("content");
                        if (content != null) return String.valueOf(content);
                    }
                }
            }

            return "[OPENAI-EMPTY]";

        } catch (IOException e) {
            return "[OPENAI-IO] " + e.getMessage();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "[OPENAI-INTERRUPTED]";
        } catch (Exception e) {
            return "[OPENAI-ERROR] " + e.getClass().getSimpleName() + ": " + e.getMessage();
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