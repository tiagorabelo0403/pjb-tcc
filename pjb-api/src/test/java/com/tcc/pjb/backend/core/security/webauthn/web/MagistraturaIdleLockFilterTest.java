package com.tcc.pjb.backend.core.security.webauthn.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.webauthn.PasskeySessionActivityService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.security.PasskeySession;
import com.tcc.pjb.backend.model.repository.security.PasskeySessionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * F5 (plano de melhoria v3): HttpServletResponse/FilterChain viram MockHttpServletResponse (fake
 * real do Spring) e lambda com flag em vez de mock -- ver MagistraturaGeofenceFilterTest para a
 * justificativa completa.
 */
class MagistraturaIdleLockFilterTest {

    private final PasskeySessionRepository repository = mock(PasskeySessionRepository.class);
    private final PasskeySessionActivityService activityService = mock(PasskeySessionActivityService.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final MagistraturaIdleLockFilter filter =
            new MagistraturaIdleLockFilter(repository, activityService, currentUserService);

    @Test
    void naoMagistraturaSegueSemChecarInatividade() throws Exception {
        when(currentUserService.getOrNull()).thenReturn(usuario(TipoUsuario.CIDADAO));
        HttpServletRequest request = mock(HttpServletRequest.class);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, resp) -> chainCalled.set(true);

        filter.doFilter(request, response, chain);

        assertThat(chainCalled.get()).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
        verify(repository, never()).findById(any());
    }

    @Test
    void magistraturaComSessaoRecenteSegueETocaASessao() throws Exception {
        when(currentUserService.getOrNull()).thenReturn(usuario(TipoUsuario.JUIZ));
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute("PJB_STRONG_AUTH_SESSION_ID")).thenReturn(10L);
        PasskeySession sessao = new PasskeySession();
        sessao.setId(10L);
        sessao.setLastSeenAt(LocalDateTime.now().minusMinutes(2));
        when(repository.findById(10L)).thenReturn(Optional.of(sessao));
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, resp) -> chainCalled.set(true);

        filter.doFilter(request, response, chain);

        assertThat(chainCalled.get()).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
        verify(activityService).touch(org.mockito.ArgumentMatchers.eq(10L), any());
    }

    @Test
    void magistraturaComSessaoInativaHaMaisDe10MinutosEBloqueada() throws Exception {
        when(currentUserService.getOrNull()).thenReturn(usuario(TipoUsuario.JUIZ));
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute("PJB_STRONG_AUTH_SESSION_ID")).thenReturn(10L);
        PasskeySession sessao = new PasskeySession();
        sessao.setId(10L);
        sessao.setLastSeenAt(LocalDateTime.now().minusMinutes(15));
        when(repository.findById(10L)).thenReturn(Optional.of(sessao));
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, resp) -> chainCalled.set(true);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        assertThat(response.getContentAsString()).contains("PJB_SESSAO_INATIVA_REAUTH_REQUIRED");
        assertThat(chainCalled.get()).isFalse();
    }

    @Test
    void promotorComSessaoInativaHaMaisDe10MinutosEBloqueada() throws Exception {
        when(currentUserService.getOrNull()).thenReturn(usuario(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO));
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute("PJB_STRONG_AUTH_SESSION_ID")).thenReturn(10L);
        PasskeySession sessao = new PasskeySession();
        sessao.setId(10L);
        sessao.setLastSeenAt(LocalDateTime.now().minusMinutes(15));
        when(repository.findById(10L)).thenReturn(Optional.of(sessao));
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, resp) -> chainCalled.set(true);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        assertThat(response.getContentAsString()).contains("PJB_SESSAO_INATIVA_REAUTH_REQUIRED");
        assertThat(chainCalled.get()).isFalse();
    }

    @Test
    void defensorComSessaoInativaHaMaisDe10MinutosEBloqueada() throws Exception {
        when(currentUserService.getOrNull()).thenReturn(usuario(TipoUsuario.DEFENSOR_PUBLICO));
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute("PJB_STRONG_AUTH_SESSION_ID")).thenReturn(11L);
        PasskeySession sessao = new PasskeySession();
        sessao.setId(11L);
        sessao.setLastSeenAt(LocalDateTime.now().minusMinutes(15));
        when(repository.findById(11L)).thenReturn(Optional.of(sessao));
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, resp) -> chainCalled.set(true);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        assertThat(response.getContentAsString()).contains("PJB_SESSAO_INATIVA_REAUTH_REQUIRED");
        assertThat(chainCalled.get()).isFalse();
    }

    private Usuario usuario(TipoUsuario tipo) {
        Usuario u = new Usuario();
        u.setId(1L);
        u.setTipoUsuario(tipo);
        return u;
    }
}
