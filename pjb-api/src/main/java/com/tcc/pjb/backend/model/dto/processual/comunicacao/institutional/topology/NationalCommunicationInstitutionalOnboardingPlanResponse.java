package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.topology;

import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalAuthenticationLanePolicyResponse;
import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalOnboardingPlanResponse(
        String affiliationId,
        String orgaoSigla,
        String orgaoNome,
        String unidadeCodigo,
        String organizationScope,
        String blueprintCode,
        String coverageMode,
        String responsibleUnitCode,
        String responsibleUnitName,
        boolean selfServiceInstitutionManagedUsers,
        boolean govBrRootIdentityRequired,
        boolean signerDualEvidenceRequired,
        boolean dualAdministrationApprovalRequired,
        List<NationalCommunicationInstitutionalOnboardingStepResponse> steps,
        List<NationalCommunicationInstitutionalAuthenticationLanePolicyResponse> lanePolicies,
        List<String> findings,
        List<String> fundamentos,
        Instant generatedAt
) {
}