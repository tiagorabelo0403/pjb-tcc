package com.tcc.pjb.backend.ai.juridica.spine;

import com.tcc.pjb.backend.model.dto.ai.legal.spine.LegalAiHallucinationGuardDescriptor;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class JuridicaAntiHallucinationProfileService {

    public LegalAiHallucinationGuardDescriptor resolve(ApiVersion version, String capability, Map<String, Object> policyVariables) {
        ApiVersion effectiveVersion = version == null ? ApiVersion.latest() : version;
        String normalizedCapability = capability == null ? "LEGAL_GENERAL_ASSIST" : capability.toUpperCase(Locale.ROOT);
        boolean discoveryCapability = normalizedCapability.contains("TRIAGE") || normalizedCapability.contains("RESEARCH");
        boolean deepReasoningCapability = normalizedCapability.contains("V3") || normalizedCapability.contains("VALIDATE") || normalizedCapability.contains("MINUTA") || normalizedCapability.contains("DRAFT");

        String citationEmissionMode = discoveryCapability
                ? "DISCOVERY_ONLY"
                : deepReasoningCapability ? "CONFIRMED_ONLY" : "CONFIRMED_OR_PENDING";

        LinkedHashMap<String, Object> policy = new LinkedHashMap<>();
        policy.put("requiresGroundedNormativeClaims", true);
        policy.put("requiresGroundedPrecedents", true);
        policy.put("requiresAuthorityFloor", true);
        policy.put("citationEmissionMode", citationEmissionMode);
        policy.put("allowPlaceholderForUnverifiedCitations", !deepReasoningCapability);
        policy.put("placeholder", JuridicaSpineLabels.UNRESOLVED_CITATION_PLACEHOLDER);
        policy.put("emitOnlyFromGroundedSets", true);
        policy.put("versionPolicy", effectiveVersion.name());
        if (policyVariables != null && !policyVariables.isEmpty()) {
            policy.put("contextPolicy", Map.copyOf(policyVariables));
        }

        return new LegalAiHallucinationGuardDescriptor(
                true,
                true,
                true,
                true,
                citationEmissionMode,
                JuridicaSpineLabels.UNRESOLVED_CITATION_PLACEHOLDER,
                List.of(
                        "MCP_LEGISLACAO",
                        "MCP_JURISPRUDENCIA",
                        "GROUNDED_RESEARCH_DOSSIER",
                        "TEMPORAL_NORMATIVE_CATALOG",
                        "VALIDATED_PRECEDENT_INDEX"
                ),
                List.of(
                        "jurisprudencia pacifica",
                        "doutrina consolidada",
                        "conforme entendimento dominante",
                        "artigo aplicavel",
                        "precedente firme",
                        "sumula aplicavel"
                ),
                Map.copyOf(policy)
        );
    }
}
