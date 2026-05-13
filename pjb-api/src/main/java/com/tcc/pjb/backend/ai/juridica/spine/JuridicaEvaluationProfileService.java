package com.tcc.pjb.backend.ai.juridica.spine;

import com.tcc.pjb.backend.model.dto.ai.legal.spine.LegalAiEvaluationDescriptor;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class JuridicaEvaluationProfileService {

    public LegalAiEvaluationDescriptor resolve(ApiVersion version, String capability, Map<String, Object> policyVariables) {
        ApiVersion effectiveVersion = version == null ? ApiVersion.latest() : version;
        LinkedHashMap<String, Object> policy = new LinkedHashMap<>();
        policy.put("groundedness", Boolean.TRUE);
        policy.put("temporalValidity", Boolean.TRUE);
        policy.put("proceduralCompatibility", effectiveVersion.isAtLeast(ApiVersion.V2));
        policy.put("citationCorrectness", policyVariables != null && Boolean.TRUE.equals(policyVariables.get("citationFirst")));
        policy.put("redTeamReady", effectiveVersion.isAtLeast(ApiVersion.V3));
        return new LegalAiEvaluationDescriptor(
                effectiveVersion.isAtLeast(ApiVersion.V3)
                        ? List.of("GROUNDING", "TEMPORALITY", "PROCEDURAL_COMPATIBILITY", "CITATION_CORRECTNESS", "SIGILO_POLICY")
                        : effectiveVersion.isAtLeast(ApiVersion.V2)
                            ? List.of("GROUNDING", "TEMPORALITY", "PROCEDURAL_COMPATIBILITY")
                            : List.of("GROUNDING", "AUTHORITY_FLOOR"),
                List.of("CIVEL", "PENAL", "TRABALHISTA", "FAZENDA", "FAMILIA", "JUIZADO"),
                effectiveVersion.isAtLeast(ApiVersion.V2),
                Map.copyOf(policy)
        );
    }
}
