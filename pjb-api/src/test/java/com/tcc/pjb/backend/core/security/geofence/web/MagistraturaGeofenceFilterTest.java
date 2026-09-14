package com.tcc.pjb.backend.core.security.geofence.web;

import static org.assertj.core.api.Assertions.assertThat;
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
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * F5 (plano de melhoria v3): a versao original mockava HttpServletResponse/FilterChain e so
 * verificava a chamada (verify(chain).doFilter(...), verify(response).setStatus(...)) -- passa
 * mesmo que o corpo/status reais estivessem errados, porque Mockito.mock() nao tem estado de
 * verdade por tras do metodo verificado. MockHttpServletResponse (fake real do Spring, nao mock)
 * guarda o status/corpo de verdade; FilterChain vira um lambda com uma flag em vez de mock, ja que
 * "a requisicao continuou" e o unico efeito observavel de um Filter e nao ha colaborador real por
 * tras para consultar.
 */
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
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, resp) -> chainCalled.set(true);

        filter.doFilter(request, response, chain);

        assertThat(chainCalled.get()).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
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
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, resp) -> chainCalled.set(true);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("PJB_GEO_FORA_DE_ESCOPO");
        assertThat(chainCalled.get()).isFalse();
    }

    @Test
    void defensorAtrasDeVpnEBloqueado() throws Exception {
        Usuario defensor = usuario(TipoUsuario.DEFENSOR_PUBLICO);
        when(currentUserService.getOrNull()).thenReturn(defensor);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(clientIpResolver.resolve(request)).thenReturn("198.51.100.9");
        when(policyService.avaliar(defensor, "198.51.100.9"))
                .thenReturn(new Avaliacao(Decisao.BLOQUEADO_VPN, "Acesso via VPN/datacenter detectado"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, resp) -> chainCalled.set(true);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("PJB_VPN_DETECTADA_DESATIVE");
        assertThat(chainCalled.get()).isFalse();
    }

    @Test
    void procuradorForaDoBrasilEBloqueado() throws Exception {
        Usuario procurador = usuario(TipoUsuario.PROCURADOR);
        when(currentUserService.getOrNull()).thenReturn(procurador);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(clientIpResolver.resolve(request)).thenReturn("203.0.113.7");
        when(policyService.avaliar(procurador, "203.0.113.7"))
                .thenReturn(new Avaliacao(Decisao.BLOQUEADO_PAIS, "Acesso fora do Brasil não autorizado"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, resp) -> chainCalled.set(true);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("PJB_GEO_FORA_DE_ESCOPO");
        assertThat(chainCalled.get()).isFalse();
    }

    @Test
    void promotorDentroDaUfDeLotacaoEPermitido() throws Exception {
        Usuario promotor = usuario(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO);
        when(currentUserService.getOrNull()).thenReturn(promotor);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(clientIpResolver.resolve(request)).thenReturn("189.1.1.1");
        when(policyService.avaliar(promotor, "189.1.1.1")).thenReturn(new Avaliacao(Decisao.PERMITIDO, null));
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, resp) -> chainCalled.set(true);

        filter.doFilter(request, response, chain);

        assertThat(chainCalled.get()).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    private Usuario usuario(TipoUsuario tipo) {
        Usuario u = new Usuario();
        u.setId(1L);
        u.setTipoUsuario(tipo);
        return u;
    }
}
