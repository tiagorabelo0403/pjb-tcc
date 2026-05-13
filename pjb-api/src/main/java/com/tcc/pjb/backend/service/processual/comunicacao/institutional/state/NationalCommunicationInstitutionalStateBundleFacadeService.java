package com.tcc.pjb.backend.service.processual.comunicacao.institutional.state;

import com.tcc.pjb.backend.core.comunicacao.institucional.entry.application.InstitutionalEntryActivationDecisionApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntryActivationBundle;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntrySummary;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalHorizontalDataPlaneApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalOperationalProfileProjectionApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalTrustGovernanceOrchestrationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalHorizontalDataPlanePlan;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOperationalProfileProjection;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalTrustGovernanceProfile;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class NationalCommunicationInstitutionalStateBundleFacadeService {

    private final InstitutionalTrustGovernanceOrchestrationApplicationService trustGovernanceOrchestrationApplicationService;
    private final InstitutionalHorizontalDataPlaneApplicationService horizontalDataPlaneApplicationService;
    private final InstitutionalOperationalProfileProjectionApplicationService operationalProfileProjectionApplicationService;
    private final InstitutionalEntryActivationDecisionApplicationService entryActivationDecisionApplicationService;

    public NationalCommunicationInstitutionalStateBundleFacadeService(
            InstitutionalTrustGovernanceOrchestrationApplicationService trustGovernanceOrchestrationApplicationService,
            InstitutionalHorizontalDataPlaneApplicationService horizontalDataPlaneApplicationService,
            InstitutionalOperationalProfileProjectionApplicationService operationalProfileProjectionApplicationService,
            InstitutionalEntryActivationDecisionApplicationService entryActivationDecisionApplicationService) {
        this.trustGovernanceOrchestrationApplicationService = Objects.requireNonNull(trustGovernanceOrchestrationApplicationService, "trustGovernanceOrchestrationApplicationService");
        this.horizontalDataPlaneApplicationService = Objects.requireNonNull(horizontalDataPlaneApplicationService, "horizontalDataPlaneApplicationService");
        this.operationalProfileProjectionApplicationService = Objects.requireNonNull(operationalProfileProjectionApplicationService, "operationalProfileProjectionApplicationService");
        this.entryActivationDecisionApplicationService = Objects.requireNonNull(entryActivationDecisionApplicationService, "entryActivationDecisionApplicationService");
    }

    public InstitutionalEntryActivationBundle avaliarEntradaAtual(String affiliationId, String nominationId) {
        return entryActivationDecisionApplicationService.avaliarEntradaAtual(affiliationId, nominationId);
    }

    public NationalCommunicationInstitutionalStateBundle carregar(String affiliationId, String nominationId) {
        InstitutionalHorizontalDataPlanePlan horizontalPlan = horizontalDataPlaneApplicationService.avaliarAtual(affiliationId, nominationId);
        InstitutionalOperationalProfileProjection operationalProfile = operationalProfileProjectionApplicationService.materializar(affiliationId, nominationId);
        InstitutionalEntryActivationBundle activationBundle = entryActivationDecisionApplicationService.avaliarEntradaAtual(affiliationId, nominationId);
        InstitutionalTrustGovernanceProfile trustProfile = trustGovernanceOrchestrationApplicationService.avaliarAtual(affiliationId, nominationId);
        return new NationalCommunicationInstitutionalStateBundle(trustProfile, horizontalPlan, operationalProfile, activationBundle);
    }

    public NationalCommunicationInstitutionalStateBundle carregar(String affiliationId, String nominationId, InstitutionalEntryActivationBundle activationBundle) {
        InstitutionalHorizontalDataPlanePlan horizontalPlan = horizontalDataPlaneApplicationService.avaliarAtual(affiliationId, nominationId);
        InstitutionalOperationalProfileProjection operationalProfile = operationalProfileProjectionApplicationService.materializar(affiliationId, nominationId);
        InstitutionalTrustGovernanceProfile trustProfile = trustGovernanceOrchestrationApplicationService.avaliarAtual(affiliationId, nominationId);
        return new NationalCommunicationInstitutionalStateBundle(trustProfile, horizontalPlan, operationalProfile, activationBundle);
    }

    public NationalCommunicationInstitutionalStateBundle carregar(InstitutionalEntrySummary summary) {
        InstitutionalEntryActivationBundle activationBundle = entryActivationDecisionApplicationService.avaliarEntradaAtual(summary);
        InstitutionalOperationalProfileProjection operationalProfile = activationBundle.operationalProfile();
        String affiliationId = prefer(
                operationalProfile == null ? null : operationalProfile.affiliationId(),
                activationBundle.decision() == null ? null : activationBundle.decision().affiliationId());
        String nominationId = prefer(
                operationalProfile == null ? null : operationalProfile.nominationId(),
                activationBundle.decision() == null ? null : activationBundle.decision().nominationId());
        InstitutionalHorizontalDataPlanePlan horizontalPlan = null;
        InstitutionalTrustGovernanceProfile trustProfile = null;
        if (affiliationId != null || nominationId != null) {
            if (operationalProfile == null) {
                operationalProfile = operationalProfileProjectionApplicationService.materializar(affiliationId, nominationId);
            }
            horizontalPlan = horizontalDataPlaneApplicationService.avaliarAtual(affiliationId, nominationId);
            trustProfile = trustGovernanceOrchestrationApplicationService.avaliarAtual(affiliationId, nominationId);
        }
        return new NationalCommunicationInstitutionalStateBundle(trustProfile, horizontalPlan, operationalProfile, activationBundle);
    }

    public InstitutionalOperationalProfileProjection materializarPerfil(String affiliationId, String nominationId) {
        return operationalProfileProjectionApplicationService.materializar(affiliationId, nominationId);
    }

    private String prefer(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary.trim();
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback.trim();
        }
        return null;
    }
}
