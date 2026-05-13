package com.tcc.pjb.backend.ai.juridica.conversation.security;

import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationToolScopeSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationTrustZoneSnapshot;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class LegalGroundingPromotionFence {

    public PromotionFenceDecision inspect(String effectiveEvidenceTier,
                                          String sourceEvidenceTier,
                                          LegalAiConversationTrustZoneSnapshot trustZone,
                                          LegalAiConversationToolScopeSnapshot toolScope) {
        LinkedHashSet<String> blockedToolIds = collectGroundingToolIds(toolScope, false);
        LinkedHashSet<String> stepUpToolIds = collectGroundingToolIds(toolScope, true);
        List<String> reasons = new ArrayList<>();
        String status;
        if ("UNTRUSTED_DOCUMENT".equalsIgnoreCase(effectiveEvidenceTier)
                || trustZone != null && "LOCKED".equalsIgnoreCase(trustZone.status())) {
            status = "BLOCKED";
            reasons.add("Grounding promotion foi bloqueada porque a cadeia efetiva de evidência caiu em documento não confiável ou zona crítica.");
        } else if ("DERIVED_DOCUMENT".equalsIgnoreCase(effectiveEvidenceTier)
                || "NO_EVIDENCE".equalsIgnoreCase(effectiveEvidenceTier)
                || trustZone != null && trustZone.sovereignBoundaryRequired() && !"OFFICIAL_DOCUMENT".equalsIgnoreCase(sourceEvidenceTier)) {
            status = "STEP_UP_REQUIRED";
            reasons.add("Grounding promotion exige step-up porque a cadeia efetiva depende de derivação, cobertura incompleta ou fonte não oficial em fronteira soberana.");
        } else {
            status = "PROMOTED";
            reasons.add("Grounding promotion foi liberada porque a cadeia efetiva permaneceu oficial e estável neste turno.");
        }
        return new PromotionFenceDecision(
                status,
                "BLOCKED".equalsIgnoreCase(status) ? List.copyOf(blockedToolIds) : List.of(),
                "STEP_UP_REQUIRED".equalsIgnoreCase(status) ? List.copyOf(stepUpToolIds) : List.of(),
                List.copyOf(reasons)
        );
    }

    private LinkedHashSet<String> collectGroundingToolIds(LegalAiConversationToolScopeSnapshot toolScope, boolean includeBlocked) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (toolScope == null) {
            return out;
        }
        addMatching(out, toolScope.allowedToolIds());
        addMatching(out, toolScope.stepUpToolIds());
        if (includeBlocked) {
            addMatching(out, toolScope.blockedToolIds());
        }
        return out;
    }

    private void addMatching(Set<String> out, List<String> toolIds) {
        if (toolIds == null) {
            return;
        }
        toolIds.stream().filter(this::isGroundingTool).forEach(out::add);
    }

    private boolean isGroundingTool(String toolId) {
        String normalized = normalize(toolId);
        return normalized != null
                && (normalized.contains("rag")
                || normalized.contains("search")
                || normalized.contains("research")
                || normalized.contains("precedent")
                || normalized.contains("ground")
                || normalized.contains("citation"));
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    public record PromotionFenceDecision(
            String status,
            List<String> blockedToolIds,
            List<String> elevatedStepUpToolIds,
            List<String> reasons
    ) {
        public PromotionFenceDecision {
            Objects.requireNonNull(status, "status");
            blockedToolIds = blockedToolIds == null ? List.of() : List.copyOf(blockedToolIds);
            elevatedStepUpToolIds = elevatedStepUpToolIds == null ? List.of() : List.copyOf(elevatedStepUpToolIds);
            reasons = reasons == null ? List.of() : List.copyOf(reasons);
        }
    }
}
