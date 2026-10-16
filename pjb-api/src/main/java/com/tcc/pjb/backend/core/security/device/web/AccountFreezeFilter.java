package com.tcc.pjb.backend.core.security.device.web;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.security.CurrentUserService;
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
                || pathMatcher.match("/actuator/health", path)
                || ("GET".equals(request.getMethod()) && "/api/v1/security/panic/status".equals(path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Optional<UserSecurityProfile> congelado = currentUserService.currentUser()
                .flatMap(user -> profileRepo.findByUsuarioId(user.getId()))
                .filter(UserSecurityProfile::isFrozenNow);
        if (congelado.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }
        UserSecurityProfile profile = congelado.get();
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/problem+json;charset=UTF-8");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "https://pjb.local/problems/account_frozen");
        body.put("title", "Account Frozen");
        body.put("status", 403);
        body.put("detail", "Conta suspensa. Contacte o administrador.");
        body.put("frozenAt", profile.getFrozenAt() != null ? profile.getFrozenAt().toString() : null);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
