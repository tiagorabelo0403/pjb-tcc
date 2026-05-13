package com.tcc.pjb.backend.service.processual.comunicacao.institutional.state;

import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntryActivationBundle;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntryActivationDecision;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalHorizontalDataPlanePlan;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOperationalProfileProjection;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalTrustGovernanceProfile;

public record NationalCommunicationInstitutionalStateBundle(
        InstitutionalTrustGovernanceProfile trustGovernanceProfile,
        InstitutionalHorizontalDataPlanePlan horizontalDataPlanePlan,
        InstitutionalOperationalProfileProjection operationalProfile,
        InstitutionalEntryActivationBundle entryActivationBundle
) {
    public String affiliationId() {
        if (operationalProfile != null && operationalProfile.affiliationId() != null && !operationalProfile.affiliationId().isBlank()) {
            return operationalProfile.affiliationId();
        }
        if (horizontalDataPlanePlan != null && horizontalDataPlanePlan.affiliationId() != null && !horizontalDataPlanePlan.affiliationId().isBlank()) {
            return horizontalDataPlanePlan.affiliationId();
        }
        if (trustGovernanceProfile != null && trustGovernanceProfile.affiliationId() != null && !trustGovernanceProfile.affiliationId().isBlank()) {
            return trustGovernanceProfile.affiliationId();
        }
        if (entryActivationDecision() != null && entryActivationDecision().affiliationId() != null && !entryActivationDecision().affiliationId().isBlank()) {
            return entryActivationDecision().affiliationId();
        }
        return null;
    }

    public String nominationId() {
        if (operationalProfile != null && operationalProfile.nominationId() != null && !operationalProfile.nominationId().isBlank()) {
            return operationalProfile.nominationId();
        }
        if (horizontalDataPlanePlan != null && horizontalDataPlanePlan.nominationId() != null && !horizontalDataPlanePlan.nominationId().isBlank()) {
            return horizontalDataPlanePlan.nominationId();
        }
        if (trustGovernanceProfile != null && trustGovernanceProfile.nominationId() != null && !trustGovernanceProfile.nominationId().isBlank()) {
            return trustGovernanceProfile.nominationId();
        }
        if (entryActivationDecision() != null && entryActivationDecision().nominationId() != null && !entryActivationDecision().nominationId().isBlank()) {
            return entryActivationDecision().nominationId();
        }
        return null;
    }

    public InstitutionalEntryActivationDecision entryActivationDecision() {
        return entryActivationBundle == null ? null : entryActivationBundle.decision();
    }
}
