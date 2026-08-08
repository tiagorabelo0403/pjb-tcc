package com.tcc.pjb.backend.core.security.webauthn.web;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.webauthn.PasskeySessionActivityService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.security.PasskeySession;
import com.tcc.pjb.backend.model.repository.security.PasskeySessionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

public class MagistraturaIdleLockFilter extends OncePerRequestFilter {

    private static final Duration LIMITE_INATIVIDADE = Duration.ofMinutes(10);
    private static final String ATTR_SESSION_ID = "PJB_STRONG_AUTH_SESSION_ID";

    private final PasskeySessionRepository passkeySessionRepository;
    private final PasskeySessionActivityService passkeySessionActivityService;
    private final CurrentUserService currentUserService;

    public MagistraturaIdleLockFilter(PasskeySessionRepository passkeySessionRepository,
                                       PasskeySessionActivityService passkeySessionActivityService,
                                       CurrentUserService currentUserService) {
        this.passkeySessionRepository = Objects.requireNonNull(passkeySessionRepository);
        this.passkeySessionActivityService = Objects.requireNonNull(passkeySessionActivityService);
        this.currentUserService = Objects.requireNonNull(currentUserService);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        Usuario u = currentUserService.getOrNull();
        if (u == null || u.getTipoUsuario() == null || !u.getTipoUsuario().isMagistratura()) {
            filterChain.doFilter(request, response);
            return;
        }

        Object sessionIdAttr = request.getAttribute(ATTR_SESSION_ID);
        if (!(sessionIdAttr instanceof Long sessionId)) {
            filterChain.doFilter(request, response);
            return;
        }

        PasskeySession sessao = passkeySessionRepository.findById(sessionId).orElse(null);
        if (sessao == null) {
            filterChain.doFilter(request, response);
            return;
        }

        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime referencia = sessao.getLastSeenAt() != null ? sessao.getLastSeenAt() : sessao.getCriadoEm();
        if (referencia != null && Duration.between(referencia, agora).compareTo(LIMITE_INATIVIDADE) > 0) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"PJB_SESSAO_INATIVA_REAUTH_REQUIRED\"}");
            return;
        }

        passkeySessionActivityService.touch(sessao.getId(), agora);
        filterChain.doFilter(request, response);
    }
}
