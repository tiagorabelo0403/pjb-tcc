package com.tcc.pjb.backend.service.security.governance;

import com.tcc.pjb.backend.configs.security.governance.ApiRouteGovernanceProperties;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Service
public class ApiSecurityGovernanceInspectorService {

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final RequestMappingHandlerMapping handlerMapping;
    private final ApiRouteGovernanceProperties routeGovernanceProperties;

    public ApiSecurityGovernanceInspectorService(RequestMappingHandlerMapping handlerMapping,
                                                ApiRouteGovernanceProperties routeGovernanceProperties) {
        this.handlerMapping = handlerMapping;
        this.routeGovernanceProperties = routeGovernanceProperties;
    }

    public ApiSecurityGovernanceReport inspect() {
        List<ApiRouteEndpointPosture> endpoints = new ArrayList<>();
        int missingMethodPolicy = 0;
        int missingRouteRule = 0;
        int endpointsUsingCatchAllPolicy = 0;
        int endpointsWithoutExplicitAccessPolicy = 0;
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMapping.getHandlerMethods().entrySet()) {
            HandlerMethod handler = entry.getValue();
            Class<?> beanType = handler.getBeanType();
            if (AnnotatedElementUtils.findMergedAnnotation(beanType, RestController.class) == null) {
                continue;
            }
            Set<String> paths = new LinkedHashSet<>(entry.getKey().getPatternValues());
            Set<String> methods = extractMethods(entry.getKey());
            Method method = handler.getMethod();
            PreAuthorize classPolicy = AnnotatedElementUtils.findMergedAnnotation(beanType, PreAuthorize.class);
            PreAuthorize methodPolicy = AnnotatedElementUtils.findMergedAnnotation(method, PreAuthorize.class);
            String classPolicyValue = classPolicy == null ? null : classPolicy.value();
            String methodPolicyValue = methodPolicy == null ? null : methodPolicy.value();
            for (String path : paths) {
                ApiRouteGovernanceProperties.Rule rule = resolveRouteRule(path);
                boolean catchAllPolicy = rule != null && "all-api".equalsIgnoreCase(rule.getName());
                boolean explicitAccessPolicy = methodPolicyValue != null || classPolicyValue != null || (rule != null && !rule.getAuthorities().isEmpty());
                if (methodPolicyValue == null && classPolicyValue == null && path.startsWith("/api/")) {
                    missingMethodPolicy++;
                }
                if (rule == null && path.startsWith("/api/")) {
                    missingRouteRule++;
                }
                if (catchAllPolicy && path.startsWith("/api/")) {
                    endpointsUsingCatchAllPolicy++;
                }
                if (!explicitAccessPolicy && path.startsWith("/api/")) {
                    endpointsWithoutExplicitAccessPolicy++;
                }
                endpoints.add(new ApiRouteEndpointPosture(
                        beanType.getSimpleName(),
                        method.getName(),
                        path,
                        methods,
                        classPolicyValue,
                        methodPolicyValue,
                        rule == null ? null : rule.getName(),
                        rule == null ? List.of() : List.copyOf(rule.getAuthorities()),
                        rule == null ? List.of() : List.copyOf(rule.getAllowedContentTypes()),
                        rule == null ? 0 : effectivePageSize(rule),
                        rule == null ? 0L : effectiveRequestBytes(rule),
                        rule == null ? 0L : effectiveOffset(rule),
                        rule == null ? 0L : rule.getMaxRequestsPerWindow(),
                        rule == null ? 0 : (rule.getRateWindowSeconds() > 0 ? rule.getRateWindowSeconds() : 60),
                        rule != null && rule.isNoStoreResponse(),
                        catchAllPolicy,
                        explicitAccessPolicy
                ));
            }
        }
        endpoints.sort(Comparator.comparing(ApiRouteEndpointPosture::path).thenComparing(ApiRouteEndpointPosture::controller).thenComparing(ApiRouteEndpointPosture::handlerMethod));
        Map<String, Long> byPolicy = new LinkedHashMap<>();
        for (ApiRouteEndpointPosture endpoint : endpoints) {
            String key = endpoint.routePolicyName() == null ? "UNMAPPED" : endpoint.routePolicyName();
            byPolicy.merge(key, 1L, Long::sum);
        }
        return new ApiSecurityGovernanceReport(
                Instant.now(),
                endpoints.size(),
                missingMethodPolicy,
                missingRouteRule,
                endpointsUsingCatchAllPolicy,
                endpointsWithoutExplicitAccessPolicy,
                byPolicy,
                endpoints
        );
    }

    private Set<String> extractMethods(RequestMappingInfo info) {
        Set<String> methods = new LinkedHashSet<>();
        info.getMethodsCondition().getMethods().forEach(method -> methods.add(method.name()));
        return methods.isEmpty() ? Set.of("ANY") : Set.copyOf(methods);
    }

    private ApiRouteGovernanceProperties.Rule resolveRouteRule(String path) {
        ApiRouteGovernanceProperties.Rule best = null;
        int bestScore = -1;
        for (ApiRouteGovernanceProperties.Rule rule : routeGovernanceProperties.getRules()) {
            for (String candidate : rule.getPaths()) {
                if (candidate == null || candidate.isBlank()) {
                    continue;
                }
                String pattern = candidate.trim();
                if (pathMatcher.match(pattern, path) && pattern.length() > bestScore) {
                    best = rule;
                    bestScore = pattern.length();
                }
            }
        }
        return best;
    }

    private int effectivePageSize(ApiRouteGovernanceProperties.Rule rule) {
        return rule.getMaxPageSize() > 0 ? rule.getMaxPageSize() : routeGovernanceProperties.getDefaultMaxPageSize();
    }

    private long effectiveRequestBytes(ApiRouteGovernanceProperties.Rule rule) {
        return rule.getMaxRequestBytes() > 0 ? rule.getMaxRequestBytes() : routeGovernanceProperties.getDefaultMaxRequestBytes();
    }

    private long effectiveOffset(ApiRouteGovernanceProperties.Rule rule) {
        return rule.getMaxOffset() >= 0 ? rule.getMaxOffset() : routeGovernanceProperties.getDefaultMaxOffset();
    }

    public record ApiSecurityGovernanceReport(
            Instant generatedAt,
            int totalEndpoints,
            int endpointsWithoutMethodPolicy,
            int endpointsWithoutRouteRule,
            int endpointsUsingCatchAllPolicy,
            int endpointsWithoutExplicitAccessPolicy,
            Map<String, Long> endpointsByRoutePolicy,
            List<ApiRouteEndpointPosture> endpoints
    ) {
    }

    public record ApiRouteEndpointPosture(
            String controller,
            String handlerMethod,
            String path,
            Set<String> methods,
            String classPreAuthorize,
            String methodPreAuthorize,
            String routePolicyName,
            List<String> routeAuthorities,
            List<String> allowedContentTypes,
            int maxPageSize,
            long maxRequestBytes,
            long maxOffset,
            long maxRequestsPerWindow,
            int rateWindowSeconds,
            boolean noStoreResponse,
            boolean catchAllRoutePolicy,
            boolean explicitAccessPolicy
    ) {
    }
}
