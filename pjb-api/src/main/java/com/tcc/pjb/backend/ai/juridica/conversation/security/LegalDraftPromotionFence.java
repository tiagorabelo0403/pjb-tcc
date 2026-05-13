package com.tcc.pjb.backend.ai.juridica.conversation.security;

import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
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
public class LegalDraftPromotionFence {

    public PromotionFenceDecision inspect(LegalAiConversationRequest request,
                                          String capability,
                                          String effectiveEvidenceTier,
                                          LegalAiConversationTrustZoneSnapshot trustZone,
                                          LegalAiConversationToolScopeSnapshot toolScope) {
        LinkedHashSet<String> blockedToolIds = collectDraftToolIds(toolScope, false);
        LinkedHashSet<String> stepUpToolIds = collectDraftToolIds(toolScope, true);
        List<String> reasons = new ArrayList<>();
        boolean mutatingDraftFlow = looksLikeDraftFlow(request, capability) || !blockedToolIds.isEmpty() || !stepUpToolIds.isEmpty();
        String status;
        if (!mutatingDraftFlow) {
            status = "NOT_REQUIRED";
            reasons.add("Draft promotion fence não precisou atuar porque o turno permaneceu fora de fluxo mutável de minuta.");
        } else if ("UNTRUSTED_DOCUMENT".equalsIgnoreCase(effectiveEvidenceTier)
                || "DERIVED_DOCUMENT".equalsIgnoreCase(effectiveEvidenceTier)
                || trustZone != null && "LOCKED".equalsIgnoreCase(trustZone.status())) {
            status = "BLOCKED";
            reasons.add("Draft promotion foi bloqueada porque minuta, protocolo ou escrita mutável não podem nascer de documento derivado, não confiável ou zona crítica.");
        } else if (trustZone != null && trustZone.sovereignBoundaryRequired()
                || "INSTITUTIONAL_CONTROLLED_DOCUMENT".equalsIgnoreCase(effectiveEvidenceTier)
                || "NO_EVIDENCE".equalsIgnoreCase(effectiveEvidenceTier)) {
            status = "STEP_UP_REQUIRED";
            reasons.add("Draft promotion exige step-up porque a cadeia soberana ainda depende de fronteira institucional, cobertura incompleta ou validação adicional.");
        } else {
            status = "PROMOTED";
            reasons.add("Draft promotion foi liberada porque a cadeia soberana permaneceu oficial e controlada para este turno mutável.");
        }
        return new PromotionFenceDecision(
                status,
                "BLOCKED".equalsIgnoreCase(status) ? List.copyOf(blockedToolIds) : List.of(),
                "STEP_UP_REQUIRED".equalsIgnoreCase(status) ? List.copyOf(stepUpToolIds.isEmpty() ? blockedToolIds : stepUpToolIds) : List.of(),
                List.copyOf(reasons)
        );
    }

    private boolean looksLikeDraftFlow(LegalAiConversationRequest request, String capability) {
        String message = normalize(request == null ? null : request.message());
        String normalizedCapability = normalize(capability);
        return (message != null && (message.contains("minuta")
                || message.contains("peti")
                || message.contains("parecer")
                || message.contains("despacho")
                || message.contains("senten")
                || message.contains("voto")
                || message.contains("protocolo")))
                || (normalizedCapability != null && (normalizedCapability.contains("draft")
                || normalizedCapability.contains("write")
                || normalizedCapability.contains("petit")
                || normalizedCapability.contains("despacho")
                || normalizedCapability.contains("sentenca")
                || normalizedCapability.contains("decisao")));
    }

    private LinkedHashSet<String> collectDraftToolIds(LegalAiConversationToolScopeSnapshot toolScope, boolean includeBlocked) {
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
        toolIds.stream().filter(this::isDraftTool).forEach(out::add);
    }

    private boolean isDraftTool(String toolId) {
        String normalized = normalize(toolId);
        return normalized != null
                && (normalized.contains("draft")
                || normalized.contains("write")
                || normalized.contains("protocol")
                || normalized.contains("petition")
                || normalized.contains("peticao")
                || normalized.contains("despacho")
                || normalized.contains("sentenca")
                || normalized.contains("decisao")
                || normalized.contains("parecer")
                || normalized.contains("minuta"));
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
