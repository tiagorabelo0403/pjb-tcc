package com.tcc.pjb.backend.ai.juridica.knowledge.support;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class LegalKnowledgeCatalogManifestService {

    private final LegalKnowledgeJsonResourceLoader resourceLoader;
    private final LegalKnowledgeSurfaceTextCatalogService surfaceTextCatalogService;
    private final LegalKnowledgeCommentaryTextCatalogService commentaryTextCatalogService;
    private volatile Map<String, Object> summary = Map.of();

    public LegalKnowledgeCatalogManifestService(LegalKnowledgeJsonResourceLoader resourceLoader,
                                                LegalKnowledgeSurfaceTextCatalogService surfaceTextCatalogService,
                                                LegalKnowledgeCommentaryTextCatalogService commentaryTextCatalogService) {
        this.resourceLoader = Objects.requireNonNull(resourceLoader, "resourceLoader");
        this.surfaceTextCatalogService = Objects.requireNonNull(surfaceTextCatalogService, "surfaceTextCatalogService");
        this.commentaryTextCatalogService = Objects.requireNonNull(commentaryTextCatalogService, "commentaryTextCatalogService");
    }

    @PostConstruct
    void validateOnStartup() {
        summary = validate();
    }

    public Map<String, Object> summary() {
        return summary;
    }

    public Map<String, Object> validate() {
        JsonNode root = resourceLoader.readTree(LegalKnowledgeResourcePaths.CATALOG_MANIFEST);
        ArrayList<String> validatedPaths = new ArrayList<>();
        ArrayList<String> missingFields = new ArrayList<>();
        JsonNode resources = root.path("requiredResources");
        if (!resources.isArray() || resources.isEmpty()) {
            throw new IllegalStateException(commentaryTextCatalogService.manifestNoRequiredResourcesMessage());
        }
        for (JsonNode item : resources) {
            String path = text(item, "path");
            String topLevel = text(item, "topLevel");
            JsonNode resource = resourceLoader.readTree(path);
            validateTopLevel(path, topLevel, resource);
            validateFields(path, resource, item.path("requiredFields"), missingFields);
            validatedPaths.add(path);
        }
        if (!missingFields.isEmpty()) {
            throw new IllegalStateException(commentaryTextCatalogService.manifestValidationFailedPrefix() + String.join(", ", missingFields));
        }
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", surfaceTextCatalogService.healthReadyStatus());
        out.put("version", text(root, "version"));
        out.put("resourceCount", validatedPaths.size());
        out.put("validatedPaths", List.copyOf(validatedPaths));
        return Collections.unmodifiableMap(out);
    }

    private void validateTopLevel(String path, String topLevel, JsonNode resource) {
        switch (topLevel) {
            case "ARRAY" -> {
                if (!resource.isArray()) {
                    throw new IllegalStateException(commentaryTextCatalogService.manifestArrayPrefix() + path);
                }
            }
            case "OBJECT" -> {
                if (!resource.isObject()) {
                    throw new IllegalStateException(commentaryTextCatalogService.manifestObjectPrefix() + path);
                }
            }
            default -> throw new IllegalStateException(commentaryTextCatalogService.manifestUnsupportedTopLevelPrefix() + path);
        }
    }

    private void validateFields(String path, JsonNode resource, JsonNode requiredFields, List<String> missingFields) {
        if (!requiredFields.isArray() || requiredFields.isEmpty()) {
            return;
        }
        if (resource.isArray()) {
            for (int index = 0; index < resource.size(); index++) {
                JsonNode item = resource.get(index);
                for (JsonNode field : requiredFields) {
                    String name = field.asText();
                    if (item.path(name).isMissingNode() || item.path(name).asText().isBlank() && !item.path(name).isArray() && !item.path(name).isObject()) {
                        missingFields.add(path + "[" + index + "]." + name);
                    }
                }
            }
            return;
        }
        for (JsonNode field : requiredFields) {
            String name = field.asText();
            if (resource.path(name).isMissingNode()) {
                missingFields.add(path + "." + name);
            }
        }
    }

    private String text(JsonNode node, String field) {
        String value = node.path(field).asText();
        return value == null ? "" : value.trim();
    }
}
