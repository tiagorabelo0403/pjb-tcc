package com.tcc.pjb.backend.core.security.webauthn.web;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Objects;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.filter.OncePerRequestFilter;
import com.tcc.pjb.backend.core.security.webauthn.PasskeySessionService;
import com.tcc.pjb.backend.model.entity.security.PasskeySession;
import com.tcc.pjb.backend.model.repository.security.PasskeySessionRepository;

public class PasskeyAuthenticationFilter extends OncePerRequestFilter {

    private final PasskeySessionRepository sessionRepo;
    private final UserDetailsService userDetailsService;

    public PasskeyAuthenticationFilter(PasskeySessionRepository sessionRepo,
                                       UserDetailsService userDetailsService) {
        this.sessionRepo = Objects.requireNonNull(sessionRepo);
        this.userDetailsService = Objects.requireNonNull(userDetailsService);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return SecurityContextHolder.getContext().getAuthentication() != null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = auth.substring("Bearer ".length()).trim();
        if (token.isBlank() || token.length() < 20 || token.length() > 2048) {
            filterChain.doFilter(request, response);
            return;
        }

        String hash = PasskeySessionService.sha256Hex(token);
        PasskeySession s = sessionRepo.findActiveByTokenHash(hash).orElse(null);
        if (s == null || s.isRevogado() || s.isExpired() || s.isTermosPendentes()) {
            filterChain.doFilter(request, response);
            return;
        }

        String email = s.getUsuario() != null ? s.getUsuario().getEmail() : null;
        if (email == null || email.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        UserDetails details = userDetailsService.loadUserByUsername(email);
        UsernamePasswordAuthenticationToken at = new UsernamePasswordAuthenticationToken(
                details, null, details.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(at);

        if (s.getDeviceId() != null) {
            request.setAttribute("PJB_DEVICE_ID", s.getDeviceId());
            request.setAttribute("PJB_STRONG_AUTH_DEVICE_ID", s.getDeviceId());
        }
        request.setAttribute("PJB_STRONG_AUTH_METHOD", "PASSKEY");
        request.setAttribute("PJB_STRONG_AUTH_SESSION_ID", s.getId());

        if (s.getScopeAction() != null) request.setAttribute("PJB_STRONG_AUTH_SCOPE_ACTION", s.getScopeAction());
        if (s.getScopeRequestHash() != null) request.setAttribute("PJB_STRONG_AUTH_SCOPE_REQ_HASH", s.getScopeRequestHash());
        request.setAttribute("PJB_STRONG_AUTH_SCOPE_ONE_SHOT", s.isScopeOneShot());

        LocalDateTime issuedAt = s.getCriadoEm() != null ? s.getCriadoEm() : LocalDateTime.now();
        request.setAttribute("PJB_STRONG_AUTH_AT", issuedAt);

        filterChain.doFilter(request, response);
    }
}
