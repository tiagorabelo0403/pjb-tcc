package com.tcc.pjb.backend.ai.juridica.knowledge.support;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class LegalKnowledgeSurfaceTextCatalogService {

    private final LegalKnowledgeJsonResourceLoader resourceLoader;
    private volatile JsonNode root;

    public LegalKnowledgeSurfaceTextCatalogService(LegalKnowledgeJsonResourceLoader resourceLoader) {
        this.resourceLoader = Objects.requireNonNull(resourceLoader, "resourceLoader");
        this.root = null;
    }

    @PostConstruct
    void load() {
        root = resourceLoader.readTree(LegalKnowledgeResourcePaths.SURFACE_TEXT_CATALOG);
    }

    public String officialReadyStatus() {
        return text("coverage", "status", "officialReady", "OFFICIAL_READY");
    }

    public String officialPlusDoctrineReadyStatus() {
        return text("coverage", "status", "officialPlusDoctrineReady", "OFFICIAL_PLUS_DOCTRINE_READY");
    }

    public String officialPrimaryMode() {
        return text("coverage", "mode", "officialPrimary", "OFFICIAL_PRIMARY_ONLY");
    }

    public String officialPlusDoctrineMode() {
        return text("coverage", "mode", "officialPlusDoctrine", "OFFICIAL_PLUS_LICENSED_DOCTRINE");
    }

    public String doctrineSeparateLanePolicyNote() {
        return text(
                "coverage",
                "policyNotes",
                "doctrineSeparateLane",
                "Doutrina deve permanecer em lane separada de legislação e jurisprudência, com peso inferior à fonte oficial quando houver conflito."
        );
    }

    public String healthReadyStatus() {
        return text("catalog", "healthStatusReady", "READY");
    }

    public String healthInvalidStatus() {
        return text("catalog", "healthStatusInvalid", "INVALID");
    }

    private String text(String section, String key, String fallback) {
        JsonNode current = root;
        if (current == null) {
            return fallback;
        }
        JsonNode sectionNode = current.path(section);
        JsonNode valueNode = sectionNode.path(key);
        String value = valueNode.asText();
        return value == null || value.isBlank() ? fallback : value.trim();
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
