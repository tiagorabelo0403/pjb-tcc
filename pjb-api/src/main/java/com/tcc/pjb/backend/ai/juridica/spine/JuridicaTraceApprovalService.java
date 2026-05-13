package com.tcc.pjb.backend.ai.juridica.spine;

import com.tcc.pjb.backend.model.dto.ai.legal.mesh.LegalAiToolDescriptor;
import com.tcc.pjb.backend.model.dto.ai.legal.spine.LegalAiApprovalDescriptor;
import com.tcc.pjb.backend.model.dto.ai.legal.spine.LegalAiTraceDescriptor;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class JuridicaTraceApprovalService {

    public LegalAiTraceDescriptor trace(ApiVersion version, String capability, Map<String, Object> policyVariables, List<LegalAiToolDescriptor> tools) {
        LinkedHashMap<String, Object> policy = new LinkedHashMap<>();
        policy.put("citationFirst", policyVariables != null && Boolean.TRUE.equals(policyVariables.get("citationFirst")));
        policy.put("symbolicValidation", true);
        policy.put("tools", tools == null ? List.of() : tools.stream().map(LegalAiToolDescriptor::id).toList());
        policy.put("capability", capability);
        policy.put("version", version == null ? ApiVersion.latest().name() : version.name());
        return new LegalAiTraceDescriptor(true, JuridicaSpineLabels.TRACE_LANE, JuridicaSpineLabels.defaultAuditFields(), Map.copyOf(policy));
    }

    public LegalAiApprovalDescriptor approval(ApiVersion version, String capability, Map<String, Object> policyVariables, List<LegalAiToolDescriptor> tools) {
        boolean sigilo = policyVariables != null && Boolean.TRUE.equals(policyVariables.get("sigilo"));
        boolean v3 = version != null && version.isAtLeast(ApiVersion.V3);
        boolean stepUp = sigilo || (tools != null && tools.stream().anyMatch(LegalAiToolDescriptor::requiresStepUp));
        boolean approvalRequired = stepUp || (capability != null && capability.toUpperCase().contains("PROTOCOLO")) || (v3 && capability != null && capability.toUpperCase().contains("DRAFT"));
        List<String> reasons = approvalRequired
                ? List.of(stepUp ? JuridicaSpineLabels.APPROVAL_STEP_UP : JuridicaSpineLabels.APPROVAL_HUMAN)
                : List.of(JuridicaSpineLabels.APPROVAL_NONE);
        LinkedHashMap<String, Object> policy = new LinkedHashMap<>();
        policy.put("humanReview", approvalRequired);
        policy.put("stepUp", stepUp);
        policy.put("mutatingToolsAllowed", false);
        policy.put("sensitiveWriteBlockedByDefault", true);
        return new LegalAiApprovalDescriptor(approvalRequired, stepUp, reasons, Map.copyOf(policy));
    }
}
