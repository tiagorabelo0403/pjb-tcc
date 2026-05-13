package com.tcc.pjb.backend.core.processual.ato;

import java.util.Objects;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleAction;

@Service
public class AtoProcessualSecurityPolicyService {

    private final AtoProcessualCatalogService atoProcessualCatalogService;

    public AtoProcessualSecurityPolicyService(AtoProcessualCatalogService atoProcessualCatalogService) {
        this.atoProcessualCatalogService = Objects.requireNonNull(atoProcessualCatalogService);
    }

    public AtoProcessualDescriptor descriptorForAction(ProcessoLifecycleAction action) {
        return atoProcessualCatalogService.descriptorFor(action);
    }

    public AtoProcessualDescriptor descriptorForActType(String actType) {
        return atoProcessualCatalogService.descriptorFor(actType);
    }

    public AtoProcessualSecurityProfile securityProfileForActType(String actType) {
        AtoProcessualDescriptor descriptor = descriptorForActType(actType);
        return descriptor != null && descriptor.securityProfile() != null
                ? descriptor.securityProfile()
                : AtoProcessualSecurityProfile.reinforced();
    }

    public String canonicalActType(String actType) {
        return atoProcessualCatalogService.canonicalActType(actType);
    }

    public boolean requiresCrossCheck(String actType) {
        return securityProfileForActType(actType).requiresCrossCheck();
    }

    public boolean requiresDecisionStepUp(String actType) {
        return securityProfileForActType(actType).requiresStepUp();
    }

    public boolean requiresBindingCheck(String actType) {
        return securityProfileForActType(actType).requiresBindingCheck();
    }
}
