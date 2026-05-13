package com.tcc.pjb.backend.ai.juridica.mcp.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.tcc.pjb.backend.ai.juridica.knowledge.support.LegalKnowledgeJsonResourceLoader;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class LegalMcpTextCatalogService {

    private final JsonNode root;
    private final Set<String> highImpactCapabilityMarkers;

    public LegalMcpTextCatalogService(LegalKnowledgeJsonResourceLoader resourceLoader) {
        Objects.requireNonNull(resourceLoader, "resourceLoader");
        this.root = resourceLoader.readTree(LegalMcpResourcePaths.TEXT_CATALOG);
        this.highImpactCapabilityMarkers = loadHighImpactMarkers();
    }

    public boolean isHighImpactCapability(String capability) {
        String normalized = normalize(capability);
        if (normalized.isEmpty()) {
            return false;
        }
        return highImpactCapabilityMarkers.stream().anyMatch(normalized::contains);
    }

    public String deliberationReasonSigiloStrong() {
        return text("deliberation", "reasons", "sigiloStrong", "Sigilo forte exige checkpoint deliberativo antes de consolidar evidência ou tese.");
    }

    public String deliberationReasonPromptInjectionOrQuarantine() {
        return text("deliberation", "reasons", "promptInjectionOrQuarantine", "Prompt injection ou quarentena documental exigem checkpoint deliberativo isolado.");
    }

    public String deliberationReasonHighImpactTask() {
        return text("deliberation", "reasons", "highImpactTask", "Tarefa jurídica de alto impacto exige checkpoint antes de fixar a combinação final de tools.");
    }

    public String deliberationReasonBenchmarkBelowExpected() {
        return text("deliberation", "reasons", "benchmarkBelowExpected", "Benchmark do plano abaixo do esperado exige revisão deliberativa das escolhas MCP.");
    }

    public String deliberationModeInlineFastPath() {
        return text("deliberation", "mode", "inlineFastPath", "INLINE_FAST_PATH");
    }

    public String deliberationModeIsolatedPolicyReview() {
        return text("deliberation", "mode", "isolatedPolicyReview", "ISOLATED_POLICY_REVIEW");
    }

    public String deliberationModeSignedAuthorityRecheck() {
        return text("deliberation", "mode", "signedAuthorityRecheck", "SIGNED_AUTHORITY_RECHECK");
    }

    public String deliberationModeThinkToolStyleCheckpoint() {
        return text("deliberation", "mode", "thinkToolStyleCheckpoint", "THINK_TOOL_STYLE_CHECKPOINT");
    }

    public String deliberationCheckpointPrefix() {
        return text("deliberation", "checkpointPrefix", "LEGAL_DELIBERATION_");
    }

    public String selectionModeIsolatedDocumentalFence() {
        return text("selection", "mode", "isolatedDocumentalFence", "ISOLATED_DOCUMENTAL_FENCE");
    }

    public String selectionModePinnedStrictTrustChain() {
        return text("selection", "mode", "pinnedStrictTrustChain", "PINNED_STRICT_TRUST_CHAIN");
    }

    public String selectionModeDiscoveryThenPin() {
        return text("selection", "mode", "discoveryThenPin", "DISCOVERY_THEN_PIN");
    }

    public String selectionModePinnedOnly() {
        return text("selection", "mode", "pinnedOnly", "PINNED_ONLY");
    }

    public String decisionReasonCapabilityPrefix() {
        return text("selection", "decisionReason", "capabilityPrefix", "capability=");
    }

    public String decisionReasonSchemaPrefix() {
        return text("selection", "decisionReason", "schemaPrefix", "schema=");
    }

    public String decisionReasonSigiloStrictTrustChain() {
        return text("selection", "decisionReason", "sigiloStrictTrustChain", "sigilo_strict_trust_chain");
    }

    public String decisionReasonAttachmentsPresent() {
        return text("selection", "decisionReason", "attachmentsPresent", "attachments_present_documental_enrichment");
    }

    public String decisionReasonPromptInjectionDetected() {
        return text("selection", "decisionReason", "promptInjectionDetected", "prompt_injection_detected_reduce_surface");
    }

    public String decisionReasonPinnedServersPrefix() {
        return text("selection", "decisionReason", "pinnedServersPrefix", "pinned_servers=");
    }

    public String decisionReasonSkillsPrefix() {
        return text("selection", "decisionReason", "skillsPrefix", "skills=");
    }

    public String decisionReasonToolExamplesPrefix() {
        return text("selection", "decisionReason", "toolExamplesPrefix", "tool_examples=");
    }

    public String decisionReasonEvidencePromotionPrefix() {
        return text("selection", "decisionReason", "evidencePromotionPrefix", "evidence_promotion=");
    }

    public String safeguardReadOnlyOnly() {
        return text("selection", "safeguard", "readOnlyOnly", "READ_ONLY_ONLY");
    }

    public String safeguardToolAnnotationsRequired() {
        return text("selection", "safeguard", "toolAnnotationsRequired", "TOOL_ANNOTATIONS_REQUIRED");
    }

    public String safeguardSignedContextRequired() {
        return text("selection", "safeguard", "signedContextRequired", "SIGNED_CONTEXT_REQUIRED");
    }

    public String safeguardCitationFirstEvidenceTrail() {
        return text("selection", "safeguard", "citationFirstEvidenceTrail", "CITATION_FIRST_EVIDENCE_TRAIL");
    }

    public String safeguardDocumentalQuarantineFence() {
        return text("selection", "safeguard", "documentalQuarantineFence", "DOCUMENTAL_QUARANTINE_FENCE");
    }

    public String safeguardFederatedAccessPolicy() {
        return text("selection", "safeguard", "federatedAccessPolicy", "FEDERATED_ACCESS_POLICY");
    }

    public String safeguardDeliberationCheckpointRequired() {
        return text("selection", "safeguard", "deliberationCheckpointRequired", "DELIBERATION_CHECKPOINT_REQUIRED");
    }

    public String safeguardContextWindowCompactionRequired() {
        return text("selection", "safeguard", "contextWindowCompactionRequired", "CONTEXT_WINDOW_COMPACTION_REQUIRED");
    }

    public String safeguardMcpDoctorReviewRequired() {
        return text("selection", "safeguard", "mcpDoctorReviewRequired", "MCP_DOCTOR_REVIEW_REQUIRED");
    }

    private Set<String> loadHighImpactMarkers() {
        JsonNode node = root.path("deliberation").path("highImpactCapabilityMarkers");
        LinkedHashSet<String> markers = new LinkedHashSet<>();
        if (node.isArray()) {
            node.forEach(item -> {
                String value = normalize(item.asText());
                if (!value.isEmpty()) {
                    markers.add(value);
                }
            });
        }
        if (markers.isEmpty()) {
            markers.add("PETICAO");
            markers.add("DECISAO");
            markers.add("DESPACHO");
            markers.add("PARECER");
        }
        return Set.copyOf(markers);
    }

    private String text(String levelOne, String levelTwo, String key, String fallback) {
        String value = root.path(levelOne).path(levelTwo).path(key).asText();
        return value == null || value.isBlank() ? fallback : value;
    }

    private String text(String levelOne, String key, String fallback) {
        String value = root.path(levelOne).path(key).asText();
        return value == null || value.isBlank() ? fallback : value;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
