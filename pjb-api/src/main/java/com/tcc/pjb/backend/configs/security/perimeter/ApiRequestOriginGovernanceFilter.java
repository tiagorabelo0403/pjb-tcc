package com.tcc.pjb.backend.configs.security.perimeter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.observability.RequestContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

public class ApiRequestOriginGovernanceFilter extends OncePerRequestFilter {

    private static final Set<String> MUTATING_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final String ATTR_MODE = "pjb.origin.governance.mode";
    private static final String ATTR_SUBJECT = "pjb.origin.governance.subject";
    private static final String ATTR_REQUIREMENT = "pjb.origin.governance.requirement";
    private static final String ATTR_CAPABILITY = "pjb.origin.governance.capability";

    private final ApiRequestOriginGovernanceProperties properties;
    private final SecurityPerimeterProperties perimeterProperties;
    private final ApiRequestOriginSignatureService signatureService;
    private final ApiRequestOriginSelectiveRequirementService selectiveRequirementService;
    private final ObjectMapper objectMapper;

    public ApiRequestOriginGovernanceFilter(ApiRequestOriginGovernanceProperties properties,
                                            SecurityPerimeterProperties perimeterProperties,
                                            ClientIpResolver clientIpResolver,
                                            ObjectMapper objectMapper) {
        this.properties = Objects.requireNonNull(properties);
        this.perimeterProperties = Objects.requireNonNull(perimeterProperties);
        this.signatureService = new ApiRequestOriginSignatureService(properties, clientIpResolver);
        this.selectiveRequirementService = new ApiRequestOriginSelectiveRequirementService(properties, objectMapper);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (request == null || !properties.isEnabled()) {
            return true;
        }
        String method = request.getMethod() == null ? "" : request.getMethod().trim().toUpperCase(Locale.ROOT);
        if (!MUTATING_METHODS.contains(method)) {
            return true;
        }
        String uri = request.getRequestURI();
        if (uri == null || uri.isBlank() || isExempt(uri)) {
            return true;
        }
        return !isGoverned(uri);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        ApiRequestOriginSelectiveRequirementService.Requirement requirement = selectiveRequirementService.resolve(request);
        boolean signedRequired = isSignedRequired(uri) || requirement.signedRequired();
        String requestOrigin = signatureService.extractRequestOrigin(request, properties.isAllowRefererFallback());
        if (isTrustedBrowserOrigin(requestOrigin) && !signedRequired) {
            stamp(request, response, "BROWSER_ALLOWLIST", requestOrigin, requirement, signedRequired);
            filterChain.doFilter(request, response);
            return;
        }
        if (signedRequired && requirement.signedRequired() && lacksSignedOriginId(request)) {
            writeProblem(response, HttpServletResponse.SC_FORBIDDEN,
                    ApiRequestOriginGovernanceMessages.CODE_SIGNED_CAPABILITY,
                    ApiRequestOriginGovernanceMessages.DETAIL_SIGNED_CAPABILITY,
                    uri);
            return;
        }
        ApiRequestOriginSignatureService.VerificationResult verification = signatureService.verify(request, requestOrigin, signedRequired);
        if (verification.allowed()) {
            stamp(request, response, "SIGNED_ATTESTATION", verification.originId(), requirement, signedRequired);
            filterChain.doFilter(request, response);
            return;
        }
        if (verification.attestationMissing()) {
            if (signedRequired && requirement.signedRequired()) {
                writeProblem(response, HttpServletResponse.SC_FORBIDDEN,
                        ApiRequestOriginGovernanceMessages.CODE_SIGNED_CAPABILITY,
                        ApiRequestOriginGovernanceMessages.DETAIL_SIGNED_CAPABILITY,
                        uri);
                return;
            }
            if (requestOrigin != null) {
                writeProblem(response, HttpServletResponse.SC_FORBIDDEN,
                        ApiRequestOriginGovernanceMessages.CODE_BROWSER_ORIGIN,
                        ApiRequestOriginGovernanceMessages.DETAIL_BROWSER_ORIGIN,
                        uri);
                return;
            }
            writeProblem(response, HttpServletResponse.SC_UNAUTHORIZED,
                    ApiRequestOriginGovernanceMessages.CODE_REQUIRED,
                    ApiRequestOriginGovernanceMessages.DETAIL_REQUIRED,
                    uri);
            return;
        }
        writeProblem(response, verification.status(), verification.code(), verification.detail(), uri);
    }

    private void stamp(HttpServletRequest request,
                       HttpServletResponse response,
                       String mode,
                       String subject,
                       ApiRequestOriginSelectiveRequirementService.Requirement requirement,
                       boolean signedRequired) {
        request.setAttribute(ATTR_MODE, mode);
        request.setAttribute(ATTR_SUBJECT, subject);
        request.setAttribute(ATTR_REQUIREMENT, requirementLabel(requirement, signedRequired));
        if (requirement != null && requirement.capability() != null) {
            request.setAttribute(ATTR_CAPABILITY, requirement.capability());
        }
        response.setHeader("X-PJB-Origin-Mode", mode);
        response.setHeader("X-PJB-Origin-Requirement", requirementLabel(requirement, signedRequired));
        if (subject != null && !subject.isBlank()) {
            response.setHeader("X-PJB-Origin-Subject", subject);
        }
        if (requirement != null && requirement.capability() != null && !requirement.capability().isBlank()) {
            response.setHeader("X-PJB-Origin-Capability", requirement.capability());
        }
        mergeVary(response, "Origin");
        mergeVary(response, "Referer");
        mergeVary(response, "X-PJB-Origin-Id");
    }

    private boolean lacksSignedOriginId(HttpServletRequest request) {
        String originId = request == null ? null : request.getHeader("X-PJB-Origin-Id");
        return originId == null || originId.isBlank();
    }

    private String requirementLabel(ApiRequestOriginSelectiveRequirementService.Requirement requirement, boolean signedRequired) {
        return signedRequired || requirement != null && requirement.signedRequired() ? "SIGNED_REQUIRED" : "BROWSER_OR_SIGNED";
    }

    private boolean isTrustedBrowserOrigin(String requestOrigin) {
        if (requestOrigin == null) {
            return false;
        }
        for (String candidate : resolveTrustedBrowserOrigins()) {
            if (requestOrigin.equalsIgnoreCase(candidate)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> resolveTrustedBrowserOrigins() {
        Set<String> resolved = new LinkedHashSet<>();
        appendTrustedOrigins(resolved, properties.getTrustedBrowserOrigins());
        if (resolved.isEmpty()) {
            appendTrustedOrigins(resolved, perimeterProperties.getCorsAllowedOrigins());
        }
        return resolved;
    }

    private void appendTrustedOrigins(Set<String> target, Iterable<String> origins) {
        if (origins == null) {
            return;
        }
        for (String origin : origins) {
            if (origin == null) {
                continue;
            }
            String normalized = origin.trim();
            if (normalized.isBlank() || "*".equals(normalized) || normalized.contains("*")) {
                continue;
            }
            target.add(normalized);
        }
    }

    private boolean isGoverned(String uri) {
        return matchesPrefix(uri, properties.getGovernedPrefixes()) || matchesPrefix(uri, properties.getSignedRequiredPrefixes());
    }

    private boolean isSignedRequired(String uri) {
        return matchesPrefix(uri, properties.getSignedRequiredPrefixes());
    }

    private boolean isExempt(String uri) {
        return matchesPrefix(uri, properties.getExemptPrefixes());
    }

    private boolean matchesPrefix(String uri, Iterable<String> prefixes) {
        if (uri == null || prefixes == null) {
            return false;
        }
        for (String prefix : prefixes) {
            if (prefix != null && !prefix.isBlank() && uri.startsWith(prefix.trim())) {
                return true;
            }
        }
        return false;
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

    private void writeProblem(HttpServletResponse response,
                              int status,
                              String type,
                              String detail,
                              String instance) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader("Cache-Control", "no-store, max-age=0");
        String requestId = RequestContext.getRequestId().orElse("");
        var payload = new java.util.LinkedHashMap<String, Object>();
        payload.put("type", ApiRequestOriginGovernanceMessages.TYPE_PREFIX + type);
        payload.put("title", ApiRequestOriginGovernanceMessages.TITLE);
        payload.put("status", status);
        payload.put("detail", detail);
        payload.put("instance", instance == null ? "" : instance);
        payload.put("requestId", requestId);
        response.getWriter().write(objectMapper.writeValueAsString(payload));
        response.getWriter().flush();
    }
}
