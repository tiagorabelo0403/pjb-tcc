package com.tcc.pjb.backend.core.security.geofence.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.security.perimeter.ClientIpResolver;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.geofence.MagistraturaGeofencePolicyService;
import com.tcc.pjb.backend.core.security.geofence.MagistraturaGeofencePolicyService.Avaliacao;
import com.tcc.pjb.backend.core.security.geofence.MagistraturaGeofencePolicyService.Decisao;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;

class MagistraturaGeofenceFilterTest {

    private final MagistraturaGeofencePolicyService policyService = mock(MagistraturaGeofencePolicyService.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final ClientIpResolver clientIpResolver = mock(ClientIpResolver.class);
    private final AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
    private final MagistraturaGeofenceFilter filter =
            new MagistraturaGeofenceFilter(policyService, currentUserService, clientIpResolver, auditLedgerService);

    @Test
    void naoElegivelSegueSemAvaliarGeofence() throws Exception {
        when(currentUserService.getOrNull()).thenReturn(usuario(TipoUsuario.CIDADAO));
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(policyService, never()).avaliar(any(), anyString());
    }

    @Test
    void promotorForaDoBrasilEBloqueado() throws Exception {
        Usuario promotor = usuario(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO);
        when(currentUserService.getOrNull()).thenReturn(promotor);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(clientIpResolver.resolve(request)).thenReturn("203.0.113.5");
        when(policyService.avaliar(promotor, "203.0.113.5"))
                .thenReturn(new Avaliacao(Decisao.BLOQUEADO_PAIS, "Acesso fora do Brasil não autorizado"));
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void defensorAtrasDeVpnEBloqueado() throws Exception {
        Usuario defensor = usuario(TipoUsuario.DEFENSOR_PUBLICO);
        when(currentUserService.getOrNull()).thenReturn(defensor);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(clientIpResolver.resolve(request)).thenReturn("198.51.100.9");
        when(policyService.avaliar(defensor, "198.51.100.9"))
                .thenReturn(new Avaliacao(Decisao.BLOQUEADO_VPN, "Acesso via VPN/datacenter detectado"));
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void procuradorForaDoBrasilEBloqueado() throws Exception {
        Usuario procurador = usuario(TipoUsuario.PROCURADOR);
        when(currentUserService.getOrNull()).thenReturn(procurador);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(clientIpResolver.resolve(request)).thenReturn("203.0.113.7");
        when(policyService.avaliar(procurador, "203.0.113.7"))
                .thenReturn(new Avaliacao(Decisao.BLOQUEADO_PAIS, "Acesso fora do Brasil não autorizado"));
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void promotorDentroDaUfDeLotacaoEPermitido() throws Exception {
        Usuario promotor = usuario(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO);
        when(currentUserService.getOrNull()).thenReturn(promotor);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(clientIpResolver.resolve(request)).thenReturn("189.1.1.1");
        when(policyService.avaliar(promotor, "189.1.1.1")).thenReturn(new Avaliacao(Decisao.PERMITIDO, null));
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response, never()).setStatus(HttpServletResponse.SC_FORBIDDEN);
    }

    private Usuario usuario(TipoUsuario tipo) {
        Usuario u = new Usuario();
        u.setId(1L);
        u.setTipoUsuario(tipo);
        return u;
    }
}
