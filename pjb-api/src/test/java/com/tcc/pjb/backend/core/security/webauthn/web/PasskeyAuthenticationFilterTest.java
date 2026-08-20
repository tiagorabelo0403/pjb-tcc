package com.tcc.pjb.backend.core.security.webauthn.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.webauthn.PasskeySessionService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.security.PasskeySession;
import com.tcc.pjb.backend.model.repository.security.PasskeySessionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

class PasskeyAuthenticationFilterTest {

    private final PasskeySessionRepository sessionRepo = mock(PasskeySessionRepository.class);
    private final UserDetailsService userDetailsService = mock(UserDetailsService.class);
    private final PasskeyAuthenticationFilter filter = new PasskeyAuthenticationFilter(sessionRepo, userDetailsService);

    @AfterEach
    void limpaContexto() {
        SecurityContextHolder.clearContext();
    }

    private PasskeySession sessao(boolean termosPendentes) {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("usuario@pjb.test");
        PasskeySession sessao = new PasskeySession();
        sessao.setUsuario(usuario);
        sessao.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        sessao.setTermosPendentes(termosPendentes);
        return sessao;
    }

    @Test
    void sessaoComTermosPendentesNuncaAutentica() throws Exception {
        String token = "a".repeat(32);
        String hash = PasskeySessionService.sha256Hex(token);
        when(sessionRepo.findActiveByTokenHash(hash)).thenReturn(Optional.of(sessao(true)));

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(chain).doFilter(request, response);
    }

    @Test
    void sessaoSemTermosPendentesAutenticaNormalmente() throws Exception {
        String token = "b".repeat(32);
        String hash = PasskeySessionService.sha256Hex(token);
        when(sessionRepo.findActiveByTokenHash(hash)).thenReturn(Optional.of(sessao(false)));
        UserDetails details = new User("usuario@pjb.test", "N/A", java.util.List.of());
        when(userDetailsService.loadUserByUsername("usuario@pjb.test")).thenReturn(details);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        verify(chain).doFilter(request, response);
    }
}
