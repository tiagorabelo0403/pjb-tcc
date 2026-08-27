package com.tcc.pjb.backend.controller.processo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.dto.processo.marketplace.MarketplaceComplementoDocumentalRequest;
import com.tcc.pjb.backend.model.dto.processo.marketplace.MarketplaceComplementoDocumentalResponse;
import com.tcc.pjb.backend.model.dto.processo.marketplace.MarketplaceProtocoloRequest;
import com.tcc.pjb.backend.model.dto.processo.marketplace.MarketplaceProtocoloResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDecision;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.service.api.oauth.MarketplaceOAuth2Service;
import com.tcc.pjb.backend.service.api.oauth.MarketplaceOAuthException;
import com.tcc.pjb.backend.service.api.surface.MarketplaceSurfaceFacadeService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

class ApiMarketplaceControllerTest {

    private final MarketplaceSurfaceFacadeService facadeService = mock(MarketplaceSurfaceFacadeService.class);
    private final CapabilityRateLimiter rateLimiter = mock(CapabilityRateLimiter.class);
    private final MarketplaceOAuth2Service marketplaceOAuth2Service = mock(MarketplaceOAuth2Service.class);
    private final ApiMarketplaceController controller =
            new ApiMarketplaceController(facadeService, rateLimiter, marketplaceOAuth2Service);

    @Test
    void protocolarUsaSempreOClientIdVerificadoPeloOAuth2NuncaOPrincipalAutenticado() {
        Authentication autenticacaoDeOutroUsuario = new UsernamePasswordAuthenticationToken("cidadao@pjb.test", null);
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        when(marketplaceOAuth2Service.authorizeHttpRequest(servletRequest, "processos:protocolar"))
                .thenReturn(new MarketplaceOAuth2Service.AuthorizedClient("integrador-real", Set.of("processos:protocolar"), "jti-1"));
        when(rateLimiter.enforce(any(), any(), anyString(), any(), any()))
                .thenReturn(new CapabilityRateLimitDecision(true, 100L, 99L, 0L, 60, 1));
        MarketplaceProtocoloRequest request = mock(MarketplaceProtocoloRequest.class);
        when(facadeService.protocolar(eq(request), eq("integrador-real")))
                .thenReturn(mock(MarketplaceProtocoloResponse.class));

        controller.protocolar(request, autenticacaoDeOutroUsuario, servletRequest);

        verify(facadeService).protocolar(request, "integrador-real");
        verify(rateLimiter).enforce(any(), any(), anyString(), any(), eq("integrador-real"));
    }

    @Test
    void protocolarPropagaFalhaQuandoEscopoInsuficiente() {
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        when(marketplaceOAuth2Service.authorizeHttpRequest(servletRequest, "processos:protocolar"))
                .thenThrow(new MarketplaceOAuthException(org.springframework.http.HttpStatus.FORBIDDEN, "Escopo insuficiente."));

        Throwable thrown = org.assertj.core.api.Assertions.catchThrowable(() ->
                controller.protocolar(mock(MarketplaceProtocoloRequest.class), null, servletRequest));

        assertThat(thrown).isInstanceOf(MarketplaceOAuthException.class);
        assertThat(((MarketplaceOAuthException) thrown).getStatus()).isEqualTo(org.springframework.http.HttpStatus.FORBIDDEN);
        verify(facadeService, org.mockito.Mockito.never()).protocolar(any(), any());
    }

    @Test
    void complementarDocumentosUsaSempreOClientIdVerificadoPeloOAuth2() {
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        when(marketplaceOAuth2Service.authorizeHttpRequest(servletRequest, "processos:documentos"))
                .thenReturn(new MarketplaceOAuth2Service.AuthorizedClient("integrador-doc", Set.of("processos:documentos"), "jti-2"));
        when(rateLimiter.enforce(any(), any(), anyString(), any(), any()))
                .thenReturn(new CapabilityRateLimitDecision(true, 100L, 99L, 0L, 60, 1));
        MarketplaceComplementoDocumentalRequest request = mock(MarketplaceComplementoDocumentalRequest.class);
        when(facadeService.complementarDocumentos(eq(7L), eq(request), eq("integrador-doc")))
                .thenReturn(mock(MarketplaceComplementoDocumentalResponse.class));

        controller.complementarDocumentos(7L, request, null, servletRequest);

        verify(facadeService).complementarDocumentos(7L, request, "integrador-doc");
    }
}
