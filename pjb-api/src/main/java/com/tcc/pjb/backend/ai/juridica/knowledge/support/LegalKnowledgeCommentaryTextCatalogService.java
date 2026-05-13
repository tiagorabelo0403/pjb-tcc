package com.tcc.pjb.backend.ai.juridica.knowledge.support;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class LegalKnowledgeCommentaryTextCatalogService {

    private final LegalKnowledgeJsonResourceLoader resourceLoader;
    private volatile JsonNode root;

    public LegalKnowledgeCommentaryTextCatalogService(LegalKnowledgeJsonResourceLoader resourceLoader) {
        this.resourceLoader = Objects.requireNonNull(resourceLoader, "resourceLoader");
        this.root = null;
    }

    @PostConstruct
    void load() {
        root = resourceLoader.readTree(LegalKnowledgeResourcePaths.COMMENTARY_TEXT_CATALOG);
    }

    public String draftSurfaceCode() {
        return text("evidence", "surfaceCode", "draft", "LEGAL_DRAFT_V2");
    }

    public String groundingSurfaceCode() {
        return text("evidence", "surfaceCode", "grounding", "LEGAL_GROUNDING_CHECK_V3");
    }

    public String promotedStatus() {
        return text("evidence", "promotionStatus", "promoted", "PROMOTED");
    }

    public String blockedStatus() {
        return text("evidence", "promotionStatus", "blocked", "BLOCKED");
    }

    public String surfacePromotionNotAnchoredReason() {
        return text("evidence", "reason", "surfacePromotionNotAnchored", "SURFACE_PROMOTION_NOT_ANCHORED");
    }

    public String draftRequirementPrefix() {
        return text("evidence", "requirementPrefix", "draft", "DRAFT_");
    }

    public String groundingRequirementPrefix() {
        return text("evidence", "requirementPrefix", "grounding", "GROUNDING_");
    }

    public String manifestNoRequiredResourcesMessage() {
        return text("manifest", "error", "noRequiredResources", "Legal knowledge catalog manifest has no required resources.");
    }

    public String manifestValidationFailedPrefix() {
        return text("manifest", "error", "validationFailedPrefix", "Legal knowledge catalog manifest validation failed: ");
    }

    public String manifestArrayPrefix() {
        return text("manifest", "error", "resourceMustBeArrayPrefix", "Legal knowledge resource must be ARRAY: ");
    }

    public String manifestObjectPrefix() {
        return text("manifest", "error", "resourceMustBeObjectPrefix", "Legal knowledge resource must be OBJECT: ");
    }

    public String manifestUnsupportedTopLevelPrefix() {
        return text("manifest", "error", "unsupportedTopLevelPrefix", "Unsupported topLevel in manifest for resource: ");
    }

    private String text(String section, String subsection, String key, String fallback) {
        JsonNode current = root;
        if (current == null) {
            return fallback;
        }
        JsonNode valueNode = current.path(section).path(subsection).path(key);
        String value = valueNode.asText();
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
