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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;

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
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
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
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(activityService).touch(org.mockito.ArgumentMatchers.eq(10L), any());
        verify(response, never()).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
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
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertThat(sw.toString()).contains("PJB_SESSAO_INATIVA_REAUTH_REQUIRED");
        verify(chain, never()).doFilter(request, response);
    }

    private Usuario usuario(TipoUsuario tipo) {
        Usuario u = new Usuario();
        u.setId(1L);
        u.setTipoUsuario(tipo);
        return u;
    }
}
