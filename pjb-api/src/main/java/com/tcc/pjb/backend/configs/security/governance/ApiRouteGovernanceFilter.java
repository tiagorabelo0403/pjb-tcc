package com.tcc.pjb.backend.configs.security.governance;

import com.tcc.pjb.backend.configs.security.perimeter.ClientIpResolver;
import com.tcc.pjb.backend.core.observability.RequestContext;
import com.tcc.pjb.backend.service.infra.scaling.JudicialScaleProfileResolver;
import com.tcc.pjb.backend.service.security.ratelimit.RateLimiterStore;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

public class ApiRouteGovernanceFilter extends OncePerRequestFilter {

    private static final List<String> PAGE_SIZE_PARAMETERS = List.of("pageSize", "size", "limit", "rows", "count");
    private static final List<String> OFFSET_PARAMETERS = List.of("offset", "start", "pageOffset", "skip");
    private static final List<String> MUTATING_METHODS = List.of("POST", "PUT", "PATCH", "DELETE");

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final ApiRouteGovernanceProperties properties;
    private final ClientIpResolver clientIpResolver;
    private final RateLimiterStore rateLimiterStore;
    private final JudicialScaleProfileResolver judicialScaleProfileResolver;

    public ApiRouteGovernanceFilter(ApiRouteGovernanceProperties properties,
                                    ClientIpResolver clientIpResolver,
                                    RateLimiterStore rateLimiterStore,
                                    JudicialScaleProfileResolver judicialScaleProfileResolver) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.clientIpResolver = Objects.requireNonNull(clientIpResolver, "clientIpResolver");
        this.rateLimiterStore = Objects.requireNonNull(rateLimiterStore, "rateLimiterStore");
        this.judicialScaleProfileResolver = Objects.requireNonNull(judicialScaleProfileResolver, "judicialScaleProfileResolver");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (!properties.isEnabled() || isExempt(uri)) {
            filterChain.doFilter(request, response);
            return;
        }
        RouteMatch match = resolveRule(uri);
        if (match == null) {
            filterChain.doFilter(request, response);
            return;
        }
        ApiRouteGovernanceProperties.Rule rule = match.rule();
        JudicialScaleProfileResolver.JudicialScalePolicy scalePolicy = judicialScaleProfileResolver.resolvePolicy(request);
        Rejection rejection = validate(rule, request);
        if (rejection == null) {
            rejection = evaluateTraffic(rule, request, scalePolicy);
        }
        if (rejection != null) {
            writeProblem(response, rejection.status(), rejection.type(), rejection.detail(), uri, rule, rejection.retryAfterSeconds(), rejection.effectiveLimit());
            return;
        }
        if (rule.isEmitHeaders()) {
            applyHeaders(request, response, rule, scalePolicy);
        }
        filterChain.doFilter(request, response);
        if (rule.isNoStoreResponse() && !response.isCommitted()) {
            response.setHeader("Cache-Control", "no-store, no-cache, max-age=0, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setDateHeader("Expires", 0L);
        }
    }

    private Rejection validate(ApiRouteGovernanceProperties.Rule rule, HttpServletRequest request) {
        if (!isMethodAllowed(rule, request.getMethod())) {
            return new Rejection(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "route_method_not_allowed", "Metodo HTTP nao permitido para a faixa governada desta API.", null, null);
        }
        if (!isContentTypeAllowed(rule, request)) {
            return new Rejection(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, "content_type_not_allowed", "Content-Type nao permitido para a faixa governada desta API.", null, null);
        }
        Rejection authorityRejection = validateAuthorities(rule);
        if (authorityRejection != null) {
            return authorityRejection;
        }
        long maxRequestBytes = resolveMaxRequestBytes(rule);
        long contentLength = request.getContentLengthLong();
        if (maxRequestBytes > 0 && contentLength > maxRequestBytes) {
            return new Rejection(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "request_body_too_large", "Corpo da requisicao excede o limite governado para esta rota.", null, null);
        }
        int maxPageSize = resolveMaxPageSize(rule);
        long pageSize = readFirstLong(request, PAGE_SIZE_PARAMETERS, -1L);
        if (pageSize > maxPageSize) {
            return new Rejection(HttpServletResponse.SC_BAD_REQUEST, "page_size_too_large", "Parametro de paginacao excede o limite governado para esta rota.", null, null);
        }
        long maxOffset = resolveMaxOffset(rule);
        long offset = readFirstLong(request, OFFSET_PARAMETERS, -1L);
        if (maxOffset >= 0 && offset > maxOffset) {
            return new Rejection(HttpServletResponse.SC_BAD_REQUEST, "offset_too_large", "Parametro de deslocamento excede o limite governado para esta rota.", null, null);
        }
        int maxPathSegments = resolveMaxPathSegments(rule);
        if (countPathSegments(request.getRequestURI()) > maxPathSegments) {
            return new Rejection(HttpServletResponse.SC_BAD_REQUEST, "path_segments_too_large", "Caminho da requisicao excede a profundidade permitida para esta rota.", null, null);
        }
        return null;
    }

    private Rejection evaluateTraffic(ApiRouteGovernanceProperties.Rule rule,
                                      HttpServletRequest request,
                                      JudicialScaleProfileResolver.JudicialScalePolicy scalePolicy) {
        long baseLimit = rule.getMaxRequestsPerWindow();
        if (baseLimit <= 0L) {
            return null;
        }
        long effectiveLimit = effectiveRateLimit(baseLimit, scalePolicy);
        int windowSeconds = rule.getRateWindowSeconds() > 0 ? rule.getRateWindowSeconds() : 60;
        String subject = resolveTrafficSubject(rule, request);
        long now = Instant.now().getEpochSecond();
        long bucket = now / windowSeconds;
        String profileKey = scalePolicy == null ? "BASE" : scalePolicy.profile().name();
        String key = "pjb:routegov:" + safe(rule.getName()) + ":p:" + profileKey + ":w" + windowSeconds + ":" + bucket + ":" + subject;
        long current = rateLimiterStore.incr(key, Duration.ofSeconds(windowSeconds + 5L));
        long remaining = Math.max(0L, effectiveLimit - current);
        long retryAfter = Math.max(1L, windowSeconds - (now % windowSeconds));
        request.setAttribute("pjb.routegov.remaining", remaining);
        request.setAttribute("pjb.routegov.retryAfter", retryAfter);
        request.setAttribute("pjb.routegov.limit.effective", effectiveLimit);
        request.setAttribute("pjb.routegov.scale.profile", scalePolicy == null ? null : scalePolicy.profile().name());
        request.setAttribute("pjb.routegov.scale.instance", scalePolicy == null ? null : scalePolicy.instanceClass());
        request.setAttribute("pjb.routegov.scale.branch", scalePolicy == null ? null : scalePolicy.branchClass());
        request.setAttribute("pjb.routegov.scale.factor", scalePolicy == null ? null : scalePolicy.rateLimitFactor());
        if (current <= effectiveLimit) {
            return null;
        }
        return new Rejection(
                429,
                "route_rate_limited",
                "Taxa de requisicoes excede a governanca permitida para esta rota.",
                retryAfter,
                effectiveLimit
        );
    }

    private long effectiveRateLimit(long baseLimit, JudicialScaleProfileResolver.JudicialScalePolicy scalePolicy) {
        double factor = scalePolicy == null ? 1d : scalePolicy.rateLimitFactor();
        return Math.max(1L, Math.round(baseLimit * factor));
    }

    private String resolveTrafficSubject(ApiRouteGovernanceProperties.Rule rule, HttpServletRequest request) {
        String strategy = rule.getRateLimitKeyStrategy();
        String ip = normalizeToken(clientIpResolver.resolve(request), "unknown");
        Authentication authentication = SecurityContextHolder.getContext() == null
                ? null
                : SecurityContextHolder.getContext().getAuthentication();
        String principal = authentication == null || !authentication.isAuthenticated()
                ? null
                : normalizeToken(authentication.getName(), null);
        if ("ip_user".equals(strategy)) {
            return principal == null ? ip : ip + ':' + principal;
        }
        if ("user".equals(strategy)) {
            return principal == null ? ip : principal;
        }
        return ip;
    }

    private boolean isContentTypeAllowed(ApiRouteGovernanceProperties.Rule rule, HttpServletRequest request) {
        if (rule.getAllowedContentTypes().isEmpty()) {
            return true;
        }
        String method = request.getMethod() == null ? "" : request.getMethod().trim().toUpperCase(Locale.ROOT);
        if (!MUTATING_METHODS.contains(method)) {
            return true;
        }
        long contentLength = request.getContentLengthLong();
        String contentType = request.getContentType();
        if ((contentType == null || contentType.isBlank()) && contentLength <= 0L) {
            return true;
        }
        if (contentType == null || contentType.isBlank()) {
            return false;
        }
        String normalized = contentType.trim().toLowerCase(Locale.ROOT);
        for (String allowed : rule.getAllowedContentTypes()) {
            if (normalized.startsWith(allowed)) {
                return true;
            }
        }
        return false;
    }

    private Rejection validateAuthorities(ApiRouteGovernanceProperties.Rule rule) {
        if (rule.getAuthorities().isEmpty()) {
            return null;
        }
        Authentication authentication = SecurityContextHolder.getContext() == null
                ? null
                : SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Set<String> required = Set.copyOf(rule.getAuthorities());
        boolean granted = authentication.getAuthorities().stream()
                .map(authority -> authority == null ? null : authority.getAuthority())
                .anyMatch(required::contains);
        if (granted) {
            return null;
        }
        return new Rejection(
                HttpServletResponse.SC_FORBIDDEN,
                "route_authority_forbidden",
                "Perfil sem autoridade suficiente para a faixa governada desta API.",
                null,
                null
        );
    }

    private void mergeVary(HttpServletResponse response, String token) {
        String current = response.getHeader("Vary");
        if (current == null || current.isBlank()) {
            response.setHeader("Vary", token);
            return;
        }
        String normalized = current.toLowerCase(Locale.ROOT);
        if (!normalized.contains(token.toLowerCase(Locale.ROOT))) {
            response.setHeader("Vary", current + ", " + token);
        }
    }

    private boolean isExempt(String uri) {
        if (uri == null || uri.isBlank()) {
            return false;
        }
        for (String prefix : properties.getExemptPrefixes()) {
            if (prefix != null && !prefix.isBlank() && uri.startsWith(prefix.trim())) {
                return true;
            }
        }
        return false;
    }

    private RouteMatch resolveRule(String uri) {
        if (uri == null || uri.isBlank()) {
            return null;
        }
        ApiRouteGovernanceProperties.Rule best = null;
        int bestScore = -1;
        for (ApiRouteGovernanceProperties.Rule rule : properties.getRules()) {
            for (String path : rule.getPaths()) {
                if (path == null || path.isBlank()) {
                    continue;
                }
                String pattern = path.trim();
                if (pathMatcher.match(pattern, uri) && pattern.length() > bestScore) {
                    best = rule;
                    bestScore = pattern.length();
                }
            }
        }
        return best == null ? null : new RouteMatch(best);
    }

    private boolean isMethodAllowed(ApiRouteGovernanceProperties.Rule rule, String method) {
        if (rule.getMethods().isEmpty()) {
            return true;
        }
        String normalized = method == null ? "" : method.trim().toUpperCase(Locale.ROOT);
        if ("OPTIONS".equals(normalized)) {
            return true;
        }
        for (String allowed : rule.getMethods()) {
            if (normalized.equalsIgnoreCase(allowed)) {
                return true;
            }
        }
        return false;
    }

    private long readFirstLong(HttpServletRequest request, List<String> names, long fallback) {
        for (String name : names) {
            String value = request.getParameter(name);
            if (value == null || value.isBlank()) {
                continue;
            }
            try {
                return Long.parseLong(value.trim());
            } catch (NumberFormatException ignored) {
                return Long.MAX_VALUE;
            }
        }
        return fallback;
    }

    private int countPathSegments(String uri) {
        if (uri == null || uri.isBlank()) {
            return 0;
        }
        int count = 0;
        for (String token : uri.split("/")) {
            if (!token.isBlank()) {
                count++;
            }
        }
        return count;
    }

    private int resolveMaxPageSize(ApiRouteGovernanceProperties.Rule rule) {
        return rule.getMaxPageSize() > 0 ? rule.getMaxPageSize() : properties.getDefaultMaxPageSize();
    }

    private long resolveMaxRequestBytes(ApiRouteGovernanceProperties.Rule rule) {
        return rule.getMaxRequestBytes() > 0 ? rule.getMaxRequestBytes() : properties.getDefaultMaxRequestBytes();
    }

    private int resolveMaxPathSegments(ApiRouteGovernanceProperties.Rule rule) {
        return rule.getMaxPathSegments() > 0 ? rule.getMaxPathSegments() : properties.getDefaultMaxPathSegments();
    }

    private long resolveMaxOffset(ApiRouteGovernanceProperties.Rule rule) {
        return rule.getMaxOffset() >= 0 ? rule.getMaxOffset() : properties.getDefaultMaxOffset();
    }

    private void applyHeaders(HttpServletRequest request,
                              HttpServletResponse response,
                              ApiRouteGovernanceProperties.Rule rule,
                              JudicialScaleProfileResolver.JudicialScalePolicy scalePolicy) {
        long effectiveLimit = request.getAttribute("pjb.routegov.limit.effective") instanceof Number number
                ? number.longValue()
                : effectiveRateLimit(rule.getMaxRequestsPerWindow(), scalePolicy);
        response.setHeader("X-PJB-Route-Policy", safe(rule.getName()));
        response.setHeader("X-PJB-Route-Max-Page-Size", String.valueOf(resolveMaxPageSize(rule)));
        response.setHeader("X-PJB-Route-Max-Request-Bytes", String.valueOf(resolveMaxRequestBytes(rule)));
        response.setHeader("X-PJB-Route-Max-Offset", String.valueOf(resolveMaxOffset(rule)));
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("X-Robots-Tag", "noindex, nofollow, noarchive");
        if (scalePolicy != null) {
            response.setHeader("X-PJB-Route-Scale-Profile", scalePolicy.profile().name());
            response.setHeader("X-PJB-Route-Scale-Instance", scalePolicy.instanceClass());
            response.setHeader("X-PJB-Route-Scale-Branch", scalePolicy.branchClass());
            response.setHeader("X-PJB-Route-Scale-Factor", formatFactor(scalePolicy.rateLimitFactor()));
        }
        if (rule.isNoStoreResponse()) {
            response.setHeader("Cache-Control", "no-store, no-cache, max-age=0, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setDateHeader("Expires", 0L);
            mergeVary(response, "Authorization");
        }
        if (effectiveLimit > 0L) {
            response.setHeader("X-PJB-Route-Rate-Limit", String.valueOf(effectiveLimit));
            response.setHeader("X-PJB-Route-Rate-Window", String.valueOf(rule.getRateWindowSeconds() > 0 ? rule.getRateWindowSeconds() : 60));
            Object remaining = request.getAttribute("pjb.routegov.remaining");
            Object retryAfter = request.getAttribute("pjb.routegov.retryAfter");
            if (remaining != null) {
                response.setHeader("RateLimit-Limit", String.valueOf(effectiveLimit));
                response.setHeader("RateLimit-Remaining", String.valueOf(remaining));
                response.setHeader("X-RateLimit-Limit", String.valueOf(effectiveLimit));
                response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
            }
            if (retryAfter != null) {
                response.setHeader("RateLimit-Reset", String.valueOf(retryAfter));
            }
        }
    }

    private void writeProblem(HttpServletResponse response,
                              int status,
                              String type,
                              String detail,
                              String instance,
                              ApiRouteGovernanceProperties.Rule rule,
                              Long retryAfterSeconds,
                              Long effectiveLimit) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader("Cache-Control", "no-store, max-age=0");
        response.setHeader("X-PJB-Route-Policy", safe(rule.getName()));
        if (retryAfterSeconds != null && retryAfterSeconds > 0L) {
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.setHeader("RateLimit-Reset", String.valueOf(retryAfterSeconds));
            response.setHeader("RateLimit-Limit", String.valueOf(effectiveLimit == null ? 0L : effectiveLimit));
            response.setHeader("RateLimit-Remaining", "0");
            response.setHeader("X-RateLimit-Limit", String.valueOf(effectiveLimit == null ? 0L : effectiveLimit));
            response.setHeader("X-RateLimit-Remaining", "0");
        }
        String requestId = RequestContext.getRequestId().orElse("");
        String body = "{" +
                "\"type\":\"https://pjb.local/problems/" + escapeJson(type) + "\"," +
                "\"title\":\"Route Governance Rejected\"," +
                "\"status\":" + status + "," +
                "\"detail\":\"" + escapeJson(detail) + "\"," +
                "\"instance\":\"" + escapeJson(instance) + "\"," +
                "\"requestId\":\"" + escapeJson(requestId) + "\"," +
                "\"policy\":\"" + escapeJson(safe(rule.getName())) + "\"," +
                "\"maxPageSize\":" + resolveMaxPageSize(rule) + "," +
                "\"maxRequestBytes\":" + resolveMaxRequestBytes(rule) + "," +
                "\"maxOffset\":" + resolveMaxOffset(rule) +
                (effectiveLimit != null ? ",\"effectiveRateLimit\":" + effectiveLimit : "") +
                (retryAfterSeconds != null ? ",\"retryAfterSeconds\":" + retryAfterSeconds : "") +
                "}";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        response.setContentLength(bytes.length);
        response.getOutputStream().write(bytes);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "unnamed" : value.trim();
    }

    private String normalizeToken(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.trim().replaceAll("[^a-zA-Z0-9_:\\.-]", "_");
        if (normalized.length() > 96) {
            normalized = normalized.substring(0, 96);
        }
        return normalized.isBlank() ? fallback : normalized;
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ")
                .replace("\r", " ");
    }

    private String formatFactor(double value) {
        return String.format(Locale.ROOT, "%.4f", value);
    }

    private record RouteMatch(ApiRouteGovernanceProperties.Rule rule) {
    }

    private record Rejection(int status, String type, String detail, Long retryAfterSeconds, Long effectiveLimit) {
    }
}
