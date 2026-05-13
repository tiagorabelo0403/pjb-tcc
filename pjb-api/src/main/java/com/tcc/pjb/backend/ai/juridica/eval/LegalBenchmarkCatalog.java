package com.tcc.pjb.backend.ai.juridica.eval;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.ai.juridica.mcp.LegalMcpServerProfile;
import com.tcc.pjb.backend.ai.juridica.mcp.support.LegalMcpResourcePaths;
import com.tcc.pjb.backend.model.dto.ai.legal.eval.LegalEvalCase;
import com.tcc.pjb.backend.model.dto.ai.legal.eval.LegalEvalSuite;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class LegalBenchmarkCatalog {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final JsonNode root;

    public LegalBenchmarkCatalog() {
        this.root = readTree(LegalMcpResourcePaths.BENCHMARK_CATALOG);
    }

    public LegalEvalSuite resolveSuite(LegalMcpServerProfile.ResolveRequest request) {
        String capability = normalize(request == null ? null : request.capability());
        String scope = resolveScope(request, capability);
        List<LegalEvalCase> cases = new ArrayList<>();
        cases.add(resolveBaselineProcessualCore(request, capability));
        if (request != null && request.attachments() != null && !request.attachments().isEmpty()) {
            cases.add(resolveAttachmentDocumentalLane(request));
        }
        if (request != null && request.sigilo()) {
            cases.add(resolveSigiloTrustChain());
        }
        if (request != null && (request.promptInjectionDetected() || request.quarantinedContext())) {
            cases.add(resolveInjectionFence());
        }
        if (requiresAuthorities(capability)) {
            cases.add(resolveAuthorityDiscovery(request, capability));
        }
        return new LegalEvalSuite(
                requiredText(root.path("suiteIdPrefix")) + scope,
                requiredText(root.path("suiteLabel")),
                scope,
                request == null || request.version() == null ? "V3" : request.version().name(),
                List.copyOf(cases)
        );
    }

    private LegalEvalCase resolveBaselineProcessualCore(LegalMcpServerProfile.ResolveRequest request, String capability) {
        JsonNode template = template("baselineProcessualCore");
        String expectedSelectionMode = null;
        if (request != null && request.promptInjectionDetected()) {
            expectedSelectionMode = optionalText(template.path("selectionModeWhenPromptInjection"));
        } else if (request != null && request.sigilo()) {
            expectedSelectionMode = optionalText(template.path("selectionModeWhenSigilo"));
        }
        return new LegalEvalCase(
                requiredText(template.path("caseId")),
                requiredText(template.path("label")),
                requiredText(template.path("description")),
                expectedSelectionMode,
                requiresProcessual(capability) ? list(template.path("requiredPinnedServersWhenProcessual")) : List.of(),
                list(template.path("requiredSafeguards")),
                integerValue(template.path("minEvidenceBudget")),
                request != null && request.promptInjectionDetected() ? integerValue(template.path("maxServerBudgetWhenPromptInjection")) : null,
                null
        );
    }

    private LegalEvalCase resolveAttachmentDocumentalLane(LegalMcpServerProfile.ResolveRequest request) {
        JsonNode template = template("attachmentDocumentalLane");
        List<String> safeguards = request != null && request.promptInjectionDetected()
                ? list(template.path("requiredSafeguardsWhenPromptInjection"))
                : list(template.path("requiredSafeguards"));
        return new LegalEvalCase(
                requiredText(template.path("caseId")),
                requiredText(template.path("label")),
                requiredText(template.path("description")),
                null,
                list(template.path("requiredPinnedServers")),
                safeguards,
                integerValue(template.path("minEvidenceBudget")),
                request != null && request.promptInjectionDetected() ? integerValue(template.path("maxServerBudgetWhenPromptInjection")) : null,
                null
        );
    }

    private LegalEvalCase resolveSigiloTrustChain() {
        JsonNode template = template("sigiloTrustChain");
        return new LegalEvalCase(
                requiredText(template.path("caseId")),
                requiredText(template.path("label")),
                requiredText(template.path("description")),
                optionalText(template.path("expectedSelectionMode")),
                list(template.path("requiredPinnedServers")),
                list(template.path("requiredSafeguards")),
                integerValue(template.path("minEvidenceBudget")),
                integerValue(template.path("maxServerBudget")),
                optionalText(template.path("expectedTrustMode"))
        );
    }

    private LegalEvalCase resolveInjectionFence() {
        JsonNode template = template("injectionFence");
        return new LegalEvalCase(
                requiredText(template.path("caseId")),
                requiredText(template.path("label")),
                requiredText(template.path("description")),
                optionalText(template.path("expectedSelectionMode")),
                list(template.path("requiredPinnedServers")),
                list(template.path("requiredSafeguards")),
                integerValue(template.path("minEvidenceBudget")),
                integerValue(template.path("maxServerBudget")),
                null
        );
    }

    private LegalEvalCase resolveAuthorityDiscovery(LegalMcpServerProfile.ResolveRequest request, String capability) {
        JsonNode template = template("authorityDiscovery");
        String expectedSelectionMode = request != null && !request.sigilo() && !request.promptInjectionDetected()
                ? optionalText(template.path("selectionModeWhenDiscoveryAllowed"))
                : null;
        return new LegalEvalCase(
                requiredText(template.path("caseId")),
                requiredText(template.path("label")),
                requiredText(template.path("description")),
                expectedSelectionMode,
                List.of(resolveAuthorityServer(capability)),
                list(template.path("requiredSafeguards")),
                integerValue(template.path("minEvidenceBudget")),
                null,
                null
        );
    }

    private JsonNode template(String key) {
        JsonNode node = root.path("templates").path(key);
        if (!node.isObject()) {
            throw new IllegalStateException("Invalid legal benchmark template: " + key);
        }
        return node;
    }

    private JsonNode readTree(String path) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return OBJECT_MAPPER.readTree(in);
        } catch (IOException e) {
            throw new IllegalStateException("Invalid legal benchmark resource: " + path, e);
        }
    }

    private List<String> list(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        LinkedHashSet<String> values = new LinkedHashSet<>();
        node.forEach(item -> {
            String value = optionalText(item);
            if (value != null && !value.isBlank()) {
                values.add(value);
            }
        });
        return List.copyOf(values);
    }

    private Integer integerValue(JsonNode node) {
        return node.isNumber() ? node.intValue() : null;
    }

    private String requiredText(JsonNode node) {
        String value = optionalText(node);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Invalid legal benchmark catalog entry: blank value");
        }
        return value;
    }

    private String optionalText(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String value = node.asText();
        return value == null || value.isBlank() ? null : value.trim();
    }

    private boolean requiresProcessual(String capability) {
        return capability.contains("PETICAO")
                || capability.contains("PARECER")
                || capability.contains("VALIDATE")
                || capability.contains("DECISAO")
                || capability.contains("DESPACHO")
                || capability.contains("PLAN");
    }

    private boolean requiresAuthorities(String capability) {
        return capability.contains("PARECER")
                || capability.contains("DECISAO")
                || capability.contains("DESPACHO")
                || capability.contains("PETICAO")
                || capability.contains("GENERAL");
    }

    private String resolveAuthorityServer(String capability) {
        if (capability.contains("DECISAO") || capability.contains("PARECER") || capability.contains("PETICAO")) {
            return "MCP_JURISPRUDENCIA";
        }
        return "MCP_LEGISLACAO";
    }

    private String resolveScope(LegalMcpServerProfile.ResolveRequest request, String capability) {
        String ramo = normalize(request == null ? null : request.ramo());
        StringBuilder scope = new StringBuilder();
        scope.append(ramo.isBlank() ? "GENERAL" : ramo);
        if (capability.contains("PETICAO")) scope.append("_PETICAO");
        else if (capability.contains("PARECER")) scope.append("_PARECER");
        else if (capability.contains("DECISAO")) scope.append("_DECISAO");
        else scope.append("_GENERAL");
        if (request != null && request.sigilo()) scope.append("_SIGILO");
        if (request != null && request.promptInjectionDetected()) scope.append("_INJECTION");
        return scope.toString();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
