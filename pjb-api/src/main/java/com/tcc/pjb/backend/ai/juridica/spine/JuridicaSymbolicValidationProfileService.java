package com.tcc.pjb.backend.ai.juridica.spine;

import com.tcc.pjb.backend.ai.juridica.symbolic.LegalSymbolicValidationCatalog;
import com.tcc.pjb.backend.model.dto.ai.legal.spine.LegalAiValidationDescriptor;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class JuridicaSymbolicValidationProfileService {

    public LegalAiValidationDescriptor resolve(ApiVersion version, String capability, Map<String, Object> policyVariables) {
        ApiVersion effectiveVersion = version == null ? ApiVersion.latest() : version;
        boolean proceduralValidation = policyVariables != null && Boolean.TRUE.equals(policyVariables.get("proceduralValidation"));
        boolean citationFirst = policyVariables != null && Boolean.TRUE.equals(policyVariables.get("citationFirst"));
        LinkedHashMap<String, Object> validationPolicy = new LinkedHashMap<>();
        validationPolicy.put("proceduralCompatibility", proceduralValidation);
        validationPolicy.put("temporalValidity", Boolean.TRUE);
        validationPolicy.put("authorityFloor", policyVariables == null ? "NORMA_CANONICA" : policyVariables.getOrDefault("authorityFloor", "NORMA_CANONICA"));
        validationPolicy.put("contradictionFence", Boolean.TRUE);
        validationPolicy.put("evidenceSufficiency", Boolean.TRUE);
        return new LegalAiValidationDescriptor(
                effectiveVersion.isAtLeast(ApiVersion.V3)
                        ? LegalSymbolicValidationCatalog.standardV3Engines()
                        : effectiveVersion.isAtLeast(ApiVersion.V2)
                            ? List.of(
                                    LegalSymbolicValidationCatalog.ENGINE_COMPETENCIA,
                                    LegalSymbolicValidationCatalog.ENGINE_CABIMENTO,
                                    LegalSymbolicValidationCatalog.ENGINE_PROCEDURAL_COMPATIBILITY
                            )
                            : List.of("EVIDENCE_SUFFICIENCY", "AUTHORITY_FLOOR"),
                true,
                citationFirst,
                Map.copyOf(validationPolicy)
        );
    }
}
