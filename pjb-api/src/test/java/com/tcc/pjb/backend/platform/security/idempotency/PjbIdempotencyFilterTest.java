package com.tcc.pjb.backend.platform.security.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.platform.security.idempotency.domain.PjbIdempotencyReplayPayload;
import jakarta.servlet.FilterChain;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class PjbIdempotencyFilterTest {

    @Test
    void shouldRejectGuardedRequestWithoutHeader() throws Exception {
        PjbIdempotencyService service = mock(PjbIdempotencyService.class);
        PjbIdempotencyFilter filter = new PjbIdempotencyFilter(service);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/peticionamento/protocolar");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, passthrough());

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getContentAsString()).contains("Idempotency-Key");
    }


    @Test
    void shouldIgnoreNonGuardedSecretariatWrite() throws Exception {
        PjbIdempotencyService service = mock(PjbIdempotencyService.class);
        PjbIdempotencyFilter filter = new PjbIdempotencyFilter(service);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/secretariat/mesa/listar");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, passthrough());

        assertThat(response.getStatus()).isEqualTo(204);
    }

    @Test
    void shouldReturnConflictWhenRequestAlreadyProcessing() throws Exception {
        PjbIdempotencyService service = mock(PjbIdempotencyService.class);
        when(service.acquire("k-1")).thenReturn(false);
        when(service.status("k-1")).thenReturn("PROCESSING");
        when(service.retryAfterSeconds()).thenReturn(5);
        PjbIdempotencyFilter filter = new PjbIdempotencyFilter(service);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/peticionamento/protocolar");
        request.addHeader("Idempotency-Key", "k-1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, passthrough());

        assertThat(response.getStatus()).isEqualTo(409);
        assertThat(response.getHeader("Retry-After")).isEqualTo("5");
    }

    @Test
    void shouldCacheSuccessfulResponseForReplay() throws Exception {
        PjbIdempotencyService service = mock(PjbIdempotencyService.class);
        when(service.acquire("k-2")).thenReturn(true);
        doNothing().when(service).complete(eq("k-2"), eq(201), eq("application/json"), eq("{\"protocol\":\"123\"}"), eq("/api/v1/peticionamento/123"));
        PjbIdempotencyFilter filter = new PjbIdempotencyFilter(service);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/peticionamento/protocolar");
        request.addHeader("Idempotency-Key", "k-2");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, successChain());

        assertThat(response.getStatus()).isEqualTo(201);
        assertThat(response.getContentAsString()).isEqualTo("{\"protocol\":\"123\"}");
        verify(service).complete("k-2", 201, "application/json", "{\"protocol\":\"123\"}", "/api/v1/peticionamento/123");
    }

    @Test
    void shouldReplayStoredResponse() throws Exception {
        PjbIdempotencyService service = mock(PjbIdempotencyService.class);
        when(service.acquire("k-3")).thenReturn(false);
        when(service.status("k-3")).thenReturn("OK");
        when(service.loadReplay("k-3")).thenReturn(Optional.of(new PjbIdempotencyReplayPayload(
                201,
                "application/json",
                "{\"protocol\":\"123\"}",
                "/api/v1/peticionamento/123",
                Instant.now()
        )));
        PjbIdempotencyFilter filter = new PjbIdempotencyFilter(service);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/peticionamento/protocolar");
        request.addHeader("Idempotency-Key", "k-3");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, passthrough());

        assertThat(response.getStatus()).isEqualTo(201);
        assertThat(response.getHeader("Location")).isEqualTo("/api/v1/peticionamento/123");
        assertThat(response.getHeader("X-Idempotent-Replay")).isEqualTo("true");
        assertThat(response.getContentAsString()).isEqualTo("{\"protocol\":\"123\"}");
    }


    @Test
    void shouldReleaseKeyWhenChainReturnsErrorStatus() throws Exception {
        PjbIdempotencyService service = mock(PjbIdempotencyService.class);
        when(service.acquire("k-5")).thenReturn(true);
        PjbIdempotencyFilter filter = new PjbIdempotencyFilter(service);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/peticionamento/protocolar");
        request.addHeader("Idempotency-Key", "k-5");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, errorChain());

        assertThat(response.getStatus()).isEqualTo(422);
        verify(service).release("k-5");
    }

    @Test
    void shouldReleaseKeyWhenChainFails() throws Exception {
        PjbIdempotencyService service = mock(PjbIdempotencyService.class);
        when(service.acquire("k-4")).thenReturn(true);
        PjbIdempotencyFilter filter = new PjbIdempotencyFilter(service);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/peticionamento/protocolar");
        request.addHeader("Idempotency-Key", "k-4");
        MockHttpServletResponse response = new MockHttpServletResponse();

        try {
            filter.doFilter(request, response, failingChain());
        } catch (IllegalStateException expected) {
            assertThat(expected).hasMessageContaining("falha");
        }

        verify(service).release("k-4");
    }

    private FilterChain passthrough() {
        return (request, response) -> ((jakarta.servlet.http.HttpServletResponse) response).setStatus(204);
    }

    private FilterChain successChain() {
        return (request, response) -> {
            jakarta.servlet.http.HttpServletResponse http = (jakarta.servlet.http.HttpServletResponse) response;
            http.setStatus(201);
            http.setContentType("application/json");
            http.setHeader("Location", "/api/v1/peticionamento/123");
            http.getWriter().write("{\"protocol\":\"123\"}");
        };
    }

    private FilterChain errorChain() {
        return (request, response) -> {
            jakarta.servlet.http.HttpServletResponse http = (jakarta.servlet.http.HttpServletResponse) response;
            http.setStatus(422);
            http.setContentType("application/json");
            http.getWriter().write("{\"error\":\"invalid\"}");
        };
    }

    private FilterChain failingChain() {
        return (request, response) -> {
            throw new IllegalStateException("falha no protocolo");
        };
    }
}
