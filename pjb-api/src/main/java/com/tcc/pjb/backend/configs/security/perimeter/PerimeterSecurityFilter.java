package com.tcc.pjb.backend.configs.security.perimeter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.service.security.SecurityBlocklistService;
import com.tcc.pjb.backend.service.security.ratelimit.RateLimitContext;
import com.tcc.pjb.backend.service.security.ratelimit.RateLimitDecision;
import com.tcc.pjb.backend.service.security.ratelimit.RateLimiterService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PerimeterSecurityFilter extends OncePerRequestFilter {

    private final SecurityPerimeterProperties properties;
    private final ClientIpResolver ipResolver;
    private final SecurityBlocklistService blocklistService;
    private final RateLimiterService rateLimiterService;
    private final ObjectMapper objectMapper;


    public PerimeterSecurityFilter(SecurityPerimeterProperties properties,
                                   ClientIpResolver ipResolver,
                                   SecurityBlocklistService blocklistService,
                                   RateLimiterService rateLimiterService,
                                   ObjectMapper objectMapper) {
        this.properties = Objects.requireNonNull(properties);
        this.ipResolver = Objects.requireNonNull(ipResolver);
        this.blocklistService = Objects.requireNonNull(blocklistService);
        this.rateLimiterService = Objects.requireNonNull(rateLimiterService);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.isEnabled();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String ip = ipResolver.resolve(request);

        
        var motivo = blocklistService.getReason(ip);
        String path = request.getRequestURI();

        if (motivo.isPresent()) {
            writeJson(response, 403, "BLOCKED", "Acesso negado pelo perímetro de segurança.", path, Map.of(
                    "ip", ip,
                    "reason", motivo.get()
            ));
            return;
        }

        
        String userKey = extractBasicAuthUser(request);
        RateLimitDecision decision = rateLimiterService.evaluate(new RateLimitContext(
                ip,
                request.getMethod(),
                request.getRequestURI(),
                userKey
        ));
        if (!decision.allowed()) {
            response.setHeader("Retry-After", String.valueOf(decision.retryAfterSeconds()));
            
            
            response.setHeader("RateLimit-Reset", String.valueOf(Math.max(0, decision.retryAfterSeconds())));
            
            response.setHeader("RateLimit-Limit", String.valueOf(decision.limit()));
            
            response.setHeader("RateLimit-Remaining", "0");

            
            response.setHeader("X-RateLimit-Limit", String.valueOf(decision.limit()));
            response.setHeader("X-RateLimit-Remaining", "0");

            writeJson(response, 429, "RATE_LIMITED", "Muitas requisições. Tente novamente mais tarde.", path, Map.of(
                    "ip", ip,
                    "limit", decision.limit(),
                    "current", decision.currentCount(),
                    "retryAfterSeconds", decision.retryAfterSeconds()
            ));
            return;
        } else if (decision.limit() > 0) {
            
            response.setHeader("RateLimit-Limit", String.valueOf(decision.limit()));
            
            response.setHeader("RateLimit-Remaining", String.valueOf(decision.remaining()));

            response.setHeader("X-RateLimit-Limit", String.valueOf(decision.limit()));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(decision.remaining()));
        }

        filterChain.doFilter(request, response);
    }

    
    private String extractBasicAuthUser(HttpServletRequest request) {
        try {
            String h = request.getHeader("Authorization");
            if (h == null || h.isBlank()) return null;
            String v = h.trim();
            if (!v.regionMatches(true, 0, "Basic ", 0, 6)) return null;
            String b64 = v.substring(6).trim();
            if (b64.isEmpty()) return null;

            byte[] decoded = Base64.getDecoder().decode(b64);
            String token = new String(decoded, StandardCharsets.UTF_8);
            int idx = token.indexOf(':');
            String user = (idx >= 0) ? token.substring(0, idx) : token;
            user = user.trim();
            if (user.isEmpty()) return null;
            if (user.length() > 64) user = user.substring(0, 64);
            return user;
        } catch (Exception ignore) {
            return null;
        }
    }

    private void writeJson(HttpServletResponse response,
                           int status,
                           String code,
                           String message,
                           String path,
                           Map<String, Object> details) throws IOException {

        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
        if (details != null && !details.isEmpty()) {
            body.put("details", details);
        }

        response.getWriter().write(objectMapper.writeValueAsString(body));
        response.getWriter().flush();

        String ip = details != null && details.get("ip") != null ? String.valueOf(details.get("ip")) : "UNKNOWN";
        log.warn("[PERIMETER] status={} code={} path={} ip={}", status, code, safePath(path), ip);
    }

    private String safePath(String path) {
        if (path == null) return "UNKNOWN";
        String p = path.trim();
        if (p.length() > 160) p = p.substring(0, 160);
        return p.replaceAll("[^a-zA-Z0-9_./-]", "");
    }
}
