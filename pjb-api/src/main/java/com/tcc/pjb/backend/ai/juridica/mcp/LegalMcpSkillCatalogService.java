package com.tcc.pjb.backend.ai.juridica.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.ai.juridica.mcp.support.LegalMcpResourcePaths;
import com.tcc.pjb.backend.model.dto.ai.legal.eval.LegalEvalReplayResult;
import com.tcc.pjb.backend.model.dto.ai.legal.mcp.LegalMcpServerDescriptor;
import com.tcc.pjb.backend.model.dto.ai.legal.mcp.LegalMcpSkillDescriptor;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class LegalMcpSkillCatalogService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final Map<String, LegalMcpSkillDescriptor> catalog;

    public LegalMcpSkillCatalogService() {
        this.catalog = loadCatalog();
    }

    public List<LegalMcpSkillDescriptor> resolve(ApiVersion version,
                                                 LegalMcpServerProfile.ResolveRequest request,
                                                 LegalEvalReplayResult evaluation,
                                                 List<LegalMcpServerDescriptor> pinnedServers) {
        Objects.requireNonNull(request, "request");
        LinkedHashSet<LegalMcpSkillDescriptor> selected = new LinkedHashSet<>();
        String capability = normalize(request.capability());
        if (requiresProcessualControl(capability)) {
            selected.add(catalog.get("LEGAL_SKILL_PRAZO_CABIMENTO_AND_COMPETENCIA"));
        }
        if (requiresAuthorityDrafting(capability, evaluation)) {
            selected.add(catalog.get("LEGAL_SKILL_CITATION_FIRST_DRAFTING"));
        }
        if (hasAttachments(request) || request.quarantinedContext()) {
            selected.add(catalog.get("LEGAL_SKILL_DOCUMENT_PROVENANCE_AND_SIGNATURE_CHAIN"));
        }
        if (request.sigilo() || request.promptInjectionDetected() || request.quarantinedContext()) {
            selected.add(catalog.get("LEGAL_SKILL_SIGILO_AND_APPROVAL_FENCE"));
        }
        if (requiresInteroperability(request, pinnedServers)) {
            selected.add(catalog.get("LEGAL_SKILL_INTEROPERABILITY_DISCOVERY_AND_GENEALOGY"));
        }
        if (selected.isEmpty()) {
            selected.add(catalog.get("LEGAL_SKILL_PRAZO_CABIMENTO_AND_COMPETENCIA"));
        }
        return selected.stream().filter(Objects::nonNull).limit(resolveBudget(request, evaluation)).toList();
    }

    public List<LegalMcpSkillDescriptor> catalog() {
        return catalog.values().stream().toList();
    }

    private Map<String, LegalMcpSkillDescriptor> loadCatalog() {
        JsonNode root = readTree(LegalMcpResourcePaths.SKILL_CATALOG);
        if (!root.isArray() || root.isEmpty()) {
            throw new IllegalStateException("Invalid legal MCP skill catalog: array required");
        }
        LinkedHashMap<String, LegalMcpSkillDescriptor> descriptors = new LinkedHashMap<>();
        for (JsonNode node : root) {
            LegalMcpSkillDescriptor descriptor = descriptor(node);
            descriptors.put(descriptor.skillId(), descriptor);
        }
        return Map.copyOf(descriptors);
    }

    private LegalMcpSkillDescriptor descriptor(JsonNode node) {
        String skillId = requiredText(node, "skillId");
        return new LegalMcpSkillDescriptor(
                skillId,
                requiredText(node, "label"),
                requiredText(node, "category"),
                requiredText(node, "activationMode"),
                node.path("sensitive").asBoolean(false),
                list(node.path("supportedCapabilities")),
                list(node.path("preferredServerIds")),
                list(node.path("preferredToolIds"))
        );
    }

    private JsonNode readTree(String path) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return OBJECT_MAPPER.readTree(in);
        } catch (IOException e) {
            throw new IllegalStateException("Invalid legal MCP resource: " + path, e);
        }
    }

    private List<String> list(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        LinkedHashSet<String> values = new LinkedHashSet<>();
        node.forEach(item -> {
            String value = requiredText(item);
            if (!value.isBlank()) {
                values.add(value);
            }
        });
        return List.copyOf(values);
    }

    private String requiredText(JsonNode node, String field) {
        return requiredText(node.path(field));
    }

    private String requiredText(JsonNode node) {
        String value = node.asText();
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Invalid legal MCP skill catalog entry: blank value");
        }
        return value.trim();
    }

    private boolean requiresProcessualControl(String capability) {
        return capability.contains("PETICAO") || capability.contains("PARECER") || capability.contains("VALIDATE") || capability.contains("PLAN") || capability.contains("CHECKLIST");
    }

    private boolean requiresAuthorityDrafting(String capability, LegalEvalReplayResult evaluation) {
        if (capability.contains("PETICAO") || capability.contains("PARECER") || capability.contains("DECISAO") || capability.contains("DESPACHO")) {
            return true;
        }
        return evaluation != null && evaluation.adaptationHints() != null && "LOAD_CANONICAL_TOOL_EXAMPLES".equals(evaluation.adaptationHints().get("toolExamplesPolicy"));
    }

    private boolean hasAttachments(LegalMcpServerProfile.ResolveRequest request) {
        return request.attachments() != null && !request.attachments().isEmpty();
    }

    private boolean requiresInteroperability(LegalMcpServerProfile.ResolveRequest request, List<LegalMcpServerDescriptor> pinnedServers) {
        if (pinnedServers != null && pinnedServers.stream().anyMatch(server -> "MCP_INTEROPERABILIDADE".equals(server.serverId()))) {
            return true;
        }
        return containsContext(request.context(), "sourceSystem", "origemSistema", "tribunalOrigem");
    }

    private int resolveBudget(LegalMcpServerProfile.ResolveRequest request, LegalEvalReplayResult evaluation) {
        if (request.promptInjectionDetected() || request.quarantinedContext()) {
            return 2;
        }
        if (evaluation != null && !evaluation.passed()) {
            return 3;
        }
        return request.sigilo() ? 4 : 3;
    }

    private boolean containsContext(Map<String, Object> context, String... keys) {
        if (context == null || context.isEmpty()) {
            return false;
        }
        for (String key : keys) {
            Object value = context.get(key);
            if (value != null && !normalize(String.valueOf(value)).isBlank()) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
