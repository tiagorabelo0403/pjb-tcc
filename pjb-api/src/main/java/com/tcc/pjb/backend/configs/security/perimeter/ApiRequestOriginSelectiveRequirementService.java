package com.tcc.pjb.backend.configs.security.perimeter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.security.device.reqhash.CachedBodyHttpServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

final class ApiRequestOriginSelectiveRequirementService {

    private static final List<String> DEFAULT_CAPABILITY_POINTERS = List.of(
            "/context/requestedCapability",
            "/context/capability",
            "/context/legalCapability",
            "/capability",
            "/contexto/requestedCapability",
            "/contexto/capability",
            "/contexto/legalCapability"
    );

    private final ApiRequestOriginGovernanceProperties properties;
    private final ObjectMapper objectMapper;

    ApiRequestOriginSelectiveRequirementService(ApiRequestOriginGovernanceProperties properties,
                                               ObjectMapper objectMapper) {
        this.properties = Objects.requireNonNull(properties);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    Requirement resolve(HttpServletRequest request) {
        if (request == null) {
            return Requirement.notRequired();
        }
        String path = normalize(request.getRequestURI());
        if (path == null) {
            return Requirement.notRequired();
        }
        for (ApiRequestOriginGovernanceProperties.SelectiveSignedRule rule : properties.getSelectiveSignedRules()) {
            if (!matches(rule, path)) {
                continue;
            }
            String capability = resolveCapability(request, rule, path);
            if (rule.getCapabilityValues().isEmpty()) {
                return Requirement.signedRequired(rule.getName(), capability);
            }
            if (capability != null && containsIgnoreCase(rule.getCapabilityValues(), capability)) {
                return Requirement.signedRequired(rule.getName(), capability);
            }
            if (capability != null) {
                return Requirement.browserOrSigned(rule.getName(), capability);
            }
        }
        return Requirement.notRequired();
    }

    private boolean matches(ApiRequestOriginGovernanceProperties.SelectiveSignedRule rule, String path) {
        if (rule == null || path == null || rule.getPaths().isEmpty()) {
            return false;
        }
        for (String candidate : rule.getPaths()) {
            if (candidate != null && !candidate.isBlank() && path.startsWith(candidate.trim())) {
                return true;
            }
        }
        return false;
    }

    private String resolveCapability(HttpServletRequest request,
                                     ApiRequestOriginGovernanceProperties.SelectiveSignedRule rule,
                                     String path) {
        String bodyCapability = resolveBodyCapability(request, rule);
        if (bodyCapability != null) {
            return bodyCapability;
        }
        return switch (path) {
            case "/api/ai/legal/minuta" -> "LEGAL_DRAFT_V2";
            case "/api/ai/legal/grounding/check" -> "LEGAL_GROUNDING_CHECK_V3";
            case "/api/ai/legal/conversation" -> "LEGAL_GENERAL_ASSIST_V3";
            default -> null;
        };
    }

    private String resolveBodyCapability(HttpServletRequest request,
                                         ApiRequestOriginGovernanceProperties.SelectiveSignedRule rule) {
        byte[] body = readBody(request);
        if (body.length == 0) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            if (root == null || root.isMissingNode() || root.isNull()) {
                return null;
            }
            List<String> pointers = rule == null || rule.getCapabilityJsonPointers().isEmpty()
                    ? DEFAULT_CAPABILITY_POINTERS
                    : rule.getCapabilityJsonPointers();
            for (String pointer : pointers) {
                String normalizedPointer = normalize(pointer);
                if (normalizedPointer == null || !normalizedPointer.startsWith("/")) {
                    continue;
                }
                JsonNode node = root.at(normalizedPointer);
                if (node != null && !node.isMissingNode() && !node.isNull() && node.isValueNode()) {
                    String capability = normalize(node.asText());
                    if (capability != null) {
                        return capability.toUpperCase(Locale.ROOT);
                    }
                }
            }
            return null;
        } catch (IOException ex) {
            return null;
        }
    }

    private byte[] readBody(HttpServletRequest request) {
        try {
            if (!(request instanceof CachedBodyHttpServletRequest) && request.getAttribute("PJB_BODY_HASH") == null) {
                return new byte[0];
            }
            return request.getInputStream().readAllBytes();
        } catch (IOException ex) {
            return new byte[0];
        }
    }

    private boolean containsIgnoreCase(List<String> candidates, String capability) {
        if (candidates == null || candidates.isEmpty() || capability == null) {
            return false;
        }
        for (String candidate : candidates) {
            if (candidate != null && capability.equalsIgnoreCase(candidate.trim())) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    record Requirement(boolean signedRequired, String ruleName, String capability) {

        static Requirement notRequired() {
            return new Requirement(false, null, null);
        }

        static Requirement browserOrSigned(String ruleName, String capability) {
            return new Requirement(false, ruleName, capability);
        }

        static Requirement signedRequired(String ruleName, String capability) {
            return new Requirement(true, ruleName, capability);
        }
    }
}
