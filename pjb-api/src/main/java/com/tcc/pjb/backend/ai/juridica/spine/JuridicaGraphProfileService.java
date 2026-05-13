package com.tcc.pjb.backend.ai.juridica.spine;

import com.tcc.pjb.backend.model.dto.ai.legal.spine.LegalAiGraphDescriptor;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class JuridicaGraphProfileService {

    public LegalAiGraphDescriptor resolve(ApiVersion version, String capability, Map<String, Object> policyVariables) {
        ApiVersion effectiveVersion = version == null ? ApiVersion.latest() : version;
        boolean graphEnabled = effectiveVersion.isAtLeast(ApiVersion.V2);
        LinkedHashMap<String, Object> policy = new LinkedHashMap<>();
        policy.put("authorityAnchored", Boolean.TRUE);
        policy.put("temporalTraversal", Boolean.TRUE);
        policy.put("branch", policyVariables == null ? "NAO_INFORMADO" : policyVariables.getOrDefault("branch", "NAO_INFORMADO"));
        policy.put("rite", policyVariables == null ? "NAO_INFORMADO" : policyVariables.getOrDefault("rite", "NAO_INFORMADO"));
        policy.put("citationFirst", policyVariables != null && Boolean.TRUE.equals(policyVariables.get("citationFirst")));
        return new LegalAiGraphDescriptor(
                graphEnabled,
                graphEnabled
                        ? effectiveVersion.isAtLeast(ApiVersion.V3)
                            ? List.of("THESE_TRAVERSAL", "PRECEDENT_CHAIN", "NORMATIVE_TEMPORAL", "PROCEDURAL_COMPATIBILITY")
                            : List.of("PRECEDENT_CHAIN", "NORMATIVE_TEMPORAL")
                        : List.of(),
                graphEnabled
                        ? List.of("NORMA", "DISPOSITIVO", "PRECEDENTE", "TESE", "RITO", "FASE", "PECA", "PRAZO", "ORGAO")
                        : List.of(),
                graphEnabled
                        ? List.of("APLICA_A", "LIMITA", "SUPERA", "DISTINGUE", "CABIVEL_EM", "INTERROMPE_PRAZO", "COMPETENTE_POR")
                        : List.of(),
                Map.copyOf(policy)
        );
    }
}
