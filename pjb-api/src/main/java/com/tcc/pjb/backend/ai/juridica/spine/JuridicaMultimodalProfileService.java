package com.tcc.pjb.backend.ai.juridica.spine;

import com.tcc.pjb.backend.model.dto.ai.legal.spine.LegalAiMultimodalDescriptor;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class JuridicaMultimodalProfileService {

    public LegalAiMultimodalDescriptor resolve(ApiVersion version, String capability, Map<String, Object> policyVariables) {
        ApiVersion effectiveVersion = version == null ? ApiVersion.latest() : version;
        boolean v3 = effectiveVersion.isAtLeast(ApiVersion.V3);
        LinkedHashMap<String, Object> policy = new LinkedHashMap<>();
        policy.put("documentQuarantineRequired", Boolean.TRUE);
        policy.put("signatureEvidenceRequired", v3);
        policy.put("sigiloAware", Boolean.TRUE);
        policy.put("provenanceEnvelope", JuridicaSpineLabels.PROVENANCE_ENVELOPE);
        policy.put("evidenceIngestionLane", JuridicaSpineLabels.MULTIMODAL_LANE);
        return new LegalAiMultimodalDescriptor(
                v3 ? List.of("TEXT", "IMAGE", "PDF", "AUDIO", "VIDEO") : List.of("TEXT", "PDF"),
                v3,
                true,
                Map.copyOf(policy)
        );
    }
}
