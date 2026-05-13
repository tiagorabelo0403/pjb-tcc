package com.tcc.pjb.backend.ai.juridica.mcp.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.tcc.pjb.backend.ai.juridica.knowledge.support.LegalKnowledgeJsonResourceLoader;
import com.tcc.pjb.backend.model.dto.ai.legal.mcp.LegalMcpToolExample;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class LegalMcpToolExampleCatalogService {

    private final Map<String, LegalMcpToolExample> examplesById;
    private final Map<String, String> exampleIdByToolId;

    public LegalMcpToolExampleCatalogService(LegalKnowledgeJsonResourceLoader resourceLoader) {
        Objects.requireNonNull(resourceLoader, "resourceLoader");
        JsonNode root = resourceLoader.readTree(LegalMcpResourcePaths.TOOL_EXAMPLE_CATALOG);
        if (!root.isArray()) {
            throw new IllegalStateException("Legal MCP tool example catalog must be ARRAY");
        }
        LinkedHashMap<String, LegalMcpToolExample> exampleCatalog = new LinkedHashMap<>();
        LinkedHashMap<String, String> toolIndex = new LinkedHashMap<>();
        for (JsonNode item : root) {
            LegalMcpToolExample example = toExample(item);
            if (exampleCatalog.putIfAbsent(example.exampleId(), example) != null) {
                throw new IllegalStateException("Duplicate Legal MCP exampleId: " + example.exampleId());
            }
            toolIndex.putIfAbsent(example.toolId(), example.exampleId());
        }
        this.examplesById = Map.copyOf(exampleCatalog);
        this.exampleIdByToolId = Map.copyOf(toolIndex);
    }

    public List<LegalMcpToolExample> catalog() {
        return new ArrayList<>(examplesById.values());
    }

    public String exampleIdForTool(String toolId) {
        return exampleIdByToolId.get(normalizeToolId(toolId));
    }

    public LegalMcpToolExample example(String exampleId) {
        return examplesById.get(normalizeId(exampleId));
    }

    private LegalMcpToolExample toExample(JsonNode item) {
        String exampleId = required(item, "exampleId");
        String toolId = required(item, "toolId");
        String title = required(item, "title");
        String usagePattern = required(item, "usagePattern");
        String invocationTemplate = required(item, "invocationTemplate");
        String safeWhen = required(item, "safeWhen");
        return new LegalMcpToolExample(normalizeId(exampleId), normalizeToolId(toolId), title, usagePattern, invocationTemplate, safeWhen);
    }

    private String required(JsonNode item, String field) {
        String value = item.path(field).asText();
        String normalized = trim(value);
        if (normalized.isEmpty()) {
            throw new IllegalStateException("Missing field in Legal MCP tool example catalog: " + field);
        }
        return normalized;
    }

    private String normalizeId(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeToolId(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
