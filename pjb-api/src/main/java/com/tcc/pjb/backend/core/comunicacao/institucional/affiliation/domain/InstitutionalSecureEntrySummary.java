package com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain;

import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntryContext;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalIdentityBaseProfile;
import java.time.Instant;
import java.util.List;

public record InstitutionalSecureEntrySummary(
        InstitutionalIdentityBaseProfile identityBaseProfile,
        InstitutionalTrustAssessment assessment,
        List<InstitutionalAffiliation> activeAffiliations,
        List<InstitutionalNomination> activeNominations,
        List<InstitutionalEntryContext> compatibleContexts,
        Instant generatedAt
) {
}
