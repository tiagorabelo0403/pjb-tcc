package com.tcc.pjb.backend.ai.juridica.spine;

import com.tcc.pjb.backend.model.dto.ai.legal.spine.LegalAiRetrievalDescriptor;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class JuridicaHybridRetrievalProfileService {

    public LegalAiRetrievalDescriptor resolve(ApiVersion version, String capability, Map<String, Object> policyVariables) {
        ApiVersion effectiveVersion = version == null ? ApiVersion.latest() : version;
        boolean citationFirst = policyVariables != null && Boolean.TRUE.equals(policyVariables.get("citationFirst"));
        LinkedHashMap<String, Object> policy = new LinkedHashMap<>();
        policy.put("authorityFloor", policyVariables == null ? "NORMA_CANONICA" : policyVariables.getOrDefault("authorityFloor", "NORMA_CANONICA"));
        policy.put("branch", policyVariables == null ? "NAO_INFORMADO" : policyVariables.getOrDefault("branch", "NAO_INFORMADO"));
        policy.put("rite", policyVariables == null ? "NAO_INFORMADO" : policyVariables.getOrDefault("rite", "NAO_INFORMADO"));
        policy.put("citationFirst", citationFirst);
        policy.put("temporalValidity", Boolean.TRUE);
        policy.put("graphExpansion", effectiveVersion.isAtLeast(ApiVersion.V2));
        policy.put("multimodalEvidence", effectiveVersion.isAtLeast(ApiVersion.V3));
        return new LegalAiRetrievalDescriptor(
                JuridicaSpineLabels.PIPELINE_LEGAL_HYBRID_RAG,
                effectiveVersion.isAtLeast(ApiVersion.V3)
                        ? List.of("STRUCTURAL_FILTER", "LEXICAL", "SEMANTIC", "RERANK", "AUTHORITY_AGGREGATION", "GRAPH_EXPANSION", "GROUNDING")
                        : effectiveVersion.isAtLeast(ApiVersion.V2)
                            ? List.of("STRUCTURAL_FILTER", "LEXICAL", "SEMANTIC", "RERANK", "AUTHORITY_AGGREGATION", "GROUNDING")
                            : List.of("STRUCTURAL_FILTER", "LEXICAL", "GROUNDING"),
                effectiveVersion.isAtLeast(ApiVersion.V2),
                effectiveVersion.isAtLeast(ApiVersion.V3),
                Map.copyOf(policy)
        );
    }
}
