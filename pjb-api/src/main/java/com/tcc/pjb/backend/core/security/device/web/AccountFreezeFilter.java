package com.tcc.pjb.backend.core.security.device.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.security.UserSecurityProfile;
import com.tcc.pjb.backend.model.repository.security.UserSecurityProfileRepository;

public class AccountFreezeFilter extends OncePerRequestFilter {

    private final CurrentUserService currentUserService;
    private final UserSecurityProfileRepository profileRepo;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public AccountFreezeFilter(CurrentUserService currentUserService,
                              UserSecurityProfileRepository profileRepo,
                              ObjectMapper objectMapper) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.profileRepo = Objects.requireNonNull(profileRepo);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return pathMatcher.match("/api/v1/public/**", path)
                || pathMatcher.match("/api/v1/auth/**", path)
                || pathMatcher.match("/actuator/health", path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            java.util.Optional<Usuario> userOpt = currentUserService.currentUser();
            if (userOpt.isPresent()) {
                Usuario user = userOpt.get();
                profileRepo.findByUsuarioId(user.getId()).ifPresent(profile -> {
                    if (profile.isFrozen()) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/problem+json;charset=UTF-8");
                        Map<String, Object> body = new LinkedHashMap<>();
                        body.put("type", "https://pjb.local/problems/account_frozen");
                        body.put("title", "Account Frozen");
                        body.put("status", 403);
                        body.put("detail", "Conta suspensa. Contacte o administrador.");
                        body.put("frozenAt", profile.getFrozenAt() != null ? profile.getFrozenAt().toString() : null);
                        try {
                            objectMapper.writeValue(response.getOutputStream(), body);
                        } catch (IOException ignored) { }
                    }
                });
            }
        } catch (Exception ignored) { }
        if (!response.isCommitted()) {
            chain.doFilter(request, response);
        }
    }
}
