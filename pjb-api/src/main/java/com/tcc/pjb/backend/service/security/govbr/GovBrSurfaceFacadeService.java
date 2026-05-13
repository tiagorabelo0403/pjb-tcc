package com.tcc.pjb.backend.service.security.govbr;

import com.tcc.pjb.backend.core.identity.govbr.application.GovBrAccountEntryGovernanceApplicationService;
import com.tcc.pjb.backend.core.identity.govbr.application.GovBrIdentityAssuranceApplicationService;
import com.tcc.pjb.backend.core.identity.govbr.domain.GovBrAccountEntryGovernanceAggregate;
import com.tcc.pjb.backend.core.identity.govbr.domain.GovBrIdentityAssuranceAggregate;
import com.tcc.pjb.backend.model.dto.security.govbr.GovBrAccountEntryGovernanceResponse;
import com.tcc.pjb.backend.model.dto.security.govbr.GovBrIdentityAssuranceResponse;
import org.springframework.stereotype.Service;

@Service
public class GovBrSurfaceFacadeService {

    private final GovBrAccountEntryGovernanceApplicationService govBrAccountEntryGovernanceApplicationService;
    private final GovBrIdentityAssuranceApplicationService govBrIdentityAssuranceApplicationService;

    public GovBrSurfaceFacadeService(GovBrAccountEntryGovernanceApplicationService govBrAccountEntryGovernanceApplicationService,
                                     GovBrIdentityAssuranceApplicationService govBrIdentityAssuranceApplicationService) {
        this.govBrAccountEntryGovernanceApplicationService = govBrAccountEntryGovernanceApplicationService;
        this.govBrIdentityAssuranceApplicationService = govBrIdentityAssuranceApplicationService;
    }

    public GovBrAccountEntryGovernanceResponse readiness() {
        return toGovernance(govBrAccountEntryGovernanceApplicationService.atual());
    }

    public GovBrAccountEntryGovernanceResponse governanca() {
        return toGovernance(govBrAccountEntryGovernanceApplicationService.atual());
    }

    public GovBrIdentityAssuranceResponse identityAssurance() {
        return toIdentityAssurance(govBrIdentityAssuranceApplicationService.atual());
    }

    private GovBrAccountEntryGovernanceResponse toGovernance(GovBrAccountEntryGovernanceAggregate aggregate) {
        return new GovBrAccountEntryGovernanceResponse(
                aggregate.enabled(),
                aggregate.mockEnabled(),
                aggregate.authorizeConfigured(),
                aggregate.tokenConfigured(),
                aggregate.userInfoConfigured(),
                aggregate.jwksConfigured(),
                aggregate.issuerConfigured(),
                aggregate.redirectPrincipalSeguro(),
                aggregate.redirectStepUpSeguro(),
                aggregate.dominiosOficiaisCompativeis(),
                aggregate.currentUserId(),
                aggregate.contaGovBrVinculada(),
                aggregate.govEmailVerificado(),
                aggregate.govTelefoneVerificado(),
                aggregate.contextoInstitucionalPronto(),
                aggregate.redirectPrincipalHost(),
                aggregate.redirectStepUpHost(),
                aggregate.contextosDelegadosAtivos(),
                aggregate.blockers(),
                aggregate.warnings(),
                aggregate.garantias(),
                aggregate.generatedAt());
    }

    private GovBrIdentityAssuranceResponse toIdentityAssurance(GovBrIdentityAssuranceAggregate aggregate) {
        return new GovBrIdentityAssuranceResponse(
                aggregate.enabled(),
                aggregate.currentUserId(),
                aggregate.contaGovBrVinculada(),
                aggregate.contextoInstitucionalFechado(),
                aggregate.callbackSeguro(),
                aggregate.dominiosOficiaisCompativeis(),
                aggregate.tokenVerificationReady(),
                aggregate.trustedDeviceAtivo(),
                aggregate.strongBindingReady(),
                aggregate.nivelGarantia(),
                aggregate.blockers(),
                aggregate.warnings(),
                aggregate.garantias(),
                aggregate.generatedAt());
    }
}
