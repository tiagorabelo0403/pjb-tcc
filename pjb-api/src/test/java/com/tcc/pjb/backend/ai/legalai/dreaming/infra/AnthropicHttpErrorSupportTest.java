package com.tcc.pjb.backend.ai.legalai.dreaming.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;

class AnthropicHttpErrorSupportTest {

    private final AnthropicHttpErrorSupport support = new AnthropicHttpErrorSupport(new ObjectMapper());
    private final RestClient.ResponseSpec.ErrorHandler handler = support.errorHandler();

    private HttpRequest request() {
        HttpRequest request = mock(HttpRequest.class);
        when(request.getURI()).thenReturn(URI.create("https://api.anthropic.com/v1/dreams"));
        return request;
    }

    private ClientHttpResponse response(HttpStatusCode status, String errorType, String message) throws Exception {
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(response.getStatusCode()).thenReturn(status);
        String json = "{\"type\":\"error\",\"error\":{\"type\":\"" + errorType + "\",\"message\":\"" + message + "\"}}";
        InputStream body = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        when(response.getBody()).thenReturn(body);
        return response;
    }

    @Test
    void status429RateLimitLancaExcecaoRetryable() throws Exception {
        assertThatThrownBy(() -> handler.handle(request(), response(HttpStatus.TOO_MANY_REQUESTS, "rate_limit_error", "limite atingido")))
                .isInstanceOf(AnthropicRetryableApiException.class)
                .satisfies(ex -> {
                    AnthropicApiErrorException typed = (AnthropicApiErrorException) ex;
                    assertThat(typed.httpStatus()).isEqualTo(429);
                    assertThat(typed.errorType()).isEqualTo("rate_limit_error");
                });
    }

    @Test
    void status500And504And529SaoRetryable() throws Exception {
        assertThatThrownBy(() -> handler.handle(request(), response(HttpStatus.INTERNAL_SERVER_ERROR, "api_error", "erro interno")))
                .isInstanceOf(AnthropicRetryableApiException.class);
        assertThatThrownBy(() -> handler.handle(request(), response(HttpStatus.GATEWAY_TIMEOUT, "timeout_error", "timeout")))
                .isInstanceOf(AnthropicRetryableApiException.class);
        assertThatThrownBy(() -> handler.handle(request(), response(HttpStatusCode.valueOf(529), "overloaded_error", "sobrecarregado")))
                .isInstanceOf(AnthropicRetryableApiException.class);
    }

    @Test
    void status400InvalidRequestLancaExcecaoNaoRetryable() throws Exception {
        assertThatThrownBy(() -> handler.handle(request(), response(HttpStatus.BAD_REQUEST, "invalid_request_error", "json invalido")))
                .isInstanceOf(AnthropicNonRetryableApiException.class)
                .satisfies(ex -> {
                    AnthropicApiErrorException typed = (AnthropicApiErrorException) ex;
                    assertThat(typed.httpStatus()).isEqualTo(400);
                    assertThat(typed.errorType()).isEqualTo("invalid_request_error");
                });
    }

    @Test
    void status401E403E404E413SaoNaoRetryable() throws Exception {
        assertThatThrownBy(() -> handler.handle(request(), response(HttpStatus.UNAUTHORIZED, "authentication_error", "chave invalida")))
                .isInstanceOf(AnthropicNonRetryableApiException.class);
        assertThatThrownBy(() -> handler.handle(request(), response(HttpStatus.FORBIDDEN, "permission_error", "sem permissao")))
                .isInstanceOf(AnthropicNonRetryableApiException.class);
        assertThatThrownBy(() -> handler.handle(request(), response(HttpStatus.NOT_FOUND, "not_found_error", "nao encontrado")))
                .isInstanceOf(AnthropicNonRetryableApiException.class);
        assertThatThrownBy(() -> handler.handle(request(), response(HttpStatus.PAYLOAD_TOO_LARGE, "request_too_large", "muito grande")))
                .isInstanceOf(AnthropicNonRetryableApiException.class);
    }

    @Test
    void corpoDeErroAusenteNaoQuebraAClassificacao() throws Exception {
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(response.getStatusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);
        when(response.getBody()).thenReturn(new ByteArrayInputStream(new byte[0]));

        assertThatThrownBy(() -> handler.handle(request(), response))
                .isInstanceOf(AnthropicRetryableApiException.class);
    }
}
