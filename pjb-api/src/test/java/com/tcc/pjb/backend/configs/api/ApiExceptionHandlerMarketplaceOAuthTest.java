package com.tcc.pjb.backend.configs.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.service.api.oauth.MarketplaceOAuthException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

/**
 * Prova que falhas de OAuth2 do marketplace (token expirado/inativo, escopo insuficiente, cliente não
 * habilitado) respondem com o status HTTP correto (401/403) — antes deste handler, essas falhas
 * usavam {@code IllegalStateException} genérica sem handler dedicado e caíam no catch-all,
 * respondendo 500 (erro interno) em vez do status correto.
 */
class ApiExceptionHandlerMarketplaceOAuthTest {

    private ApiExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        ObjectProvider<Object> emptyProvider = mock(ObjectProvider.class);
        when(emptyProvider.getIfAvailable()).thenReturn(null);
        handler = new ApiExceptionHandler(
                (ObjectProvider) emptyProvider,
                (ObjectProvider) emptyProvider);
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/marketplace/v1/processos");
    }

    @Test
    void tokenExpiradoRespondeUnauthorized() {
        ResponseEntity<ProblemDetail> resp = handler.handleMarketplaceOAuth(
                new MarketplaceOAuthException(HttpStatus.UNAUTHORIZED, "Token expirado."), request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resp.getBody().getDetail()).isEqualTo("Token expirado.");
    }

    @Test
    void escopoInsuficienteRespondeForbidden() {
        ResponseEntity<ProblemDetail> resp = handler.handleMarketplaceOAuth(
                new MarketplaceOAuthException(HttpStatus.FORBIDDEN, "Escopo insuficiente."), request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resp.getBody().getDetail()).isEqualTo("Escopo insuficiente.");
    }
}
