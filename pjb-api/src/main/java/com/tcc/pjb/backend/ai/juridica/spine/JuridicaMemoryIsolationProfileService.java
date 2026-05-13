package com.tcc.pjb.backend.ai.juridica.spine;

import com.tcc.pjb.backend.model.dto.ai.legal.spine.LegalAiMemoryScopeDescriptor;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class JuridicaMemoryIsolationProfileService {

    public LegalAiMemoryScopeDescriptor resolve(ApiVersion version, String capability, Map<String, Object> policyVariables) {
        ApiVersion effectiveVersion = version == null ? ApiVersion.latest() : version;
        boolean sigilo = policyVariables != null && Boolean.TRUE.equals(policyVariables.get("sigilo"));
        LinkedHashMap<String, Object> policy = new LinkedHashMap<>();
        policy.put("caseIsolation", Boolean.TRUE);
        policy.put("crossCaseReuseBlocked", Boolean.TRUE);
        policy.put("sigiloAware", sigilo);
        policy.put("institutionalMemory", Boolean.TRUE);
        policy.put("sessionTtlMinutes", effectiveVersion.isAtLeast(ApiVersion.V3) ? 45 : 20);
        return new LegalAiMemoryScopeDescriptor(
                effectiveVersion.isAtLeast(ApiVersion.V2)
                        ? List.of("INSTITUTIONAL", "PROCESSO", "PERFIL", "SESSAO")
                        : List.of("INSTITUTIONAL", "SESSAO"),
                true,
                true,
                Map.copyOf(policy)
        );
    }
}
