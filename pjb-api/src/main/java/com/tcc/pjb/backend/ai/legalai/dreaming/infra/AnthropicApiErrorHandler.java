package com.tcc.pjb.backend.ai.legalai.dreaming.infra;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;

final class AnthropicApiErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(AnthropicApiErrorHandler.class);

    private static final Set<Integer> RETRYABLE_STATUS = Set.of(429, 500, 502, 503, 504, 529);

    private final ObjectMapper objectMapper;

    AnthropicApiErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    RestClient.ResponseSpec.ErrorHandler errorHandler() {
        return this::handle;
    }

    private void handle(HttpRequest request, ClientHttpResponse response) throws IOException {
        int status = response.getStatusCode().value();
        AnthropicErrorBody body = readBody(response);
        String errorType = body != null && body.error() != null ? body.error().type() : "unknown_error";
        String message = body != null && body.error() != null ? body.error().message() : "Sem corpo de erro na resposta.";

        log.warn("[ANTHROPIC-API] status={} type={} uri={} message={}", status, errorType, request.getURI(), message);

        if (RETRYABLE_STATUS.contains(status)) {
            throw new AnthropicRetryableApiException(status, errorType, message);
        }
        throw new AnthropicNonRetryableApiException(status, errorType, message);
    }

    private AnthropicErrorBody readBody(ClientHttpResponse response) {
        try {
            byte[] raw = response.getBody().readAllBytes();
            if (raw.length == 0) {
                return null;
            }
            return objectMapper.readValue(new String(raw, StandardCharsets.UTF_8), AnthropicErrorBody.class);
        } catch (IOException e) {
            return null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AnthropicErrorBody(String type, ErrorDetail error) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        record ErrorDetail(String type, String message) {
        }
    }
}
