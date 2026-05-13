package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry;

import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.affiliation.NationalCommunicationInstitutionalAffiliationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalNominationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalTrustAssessmentResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalIdentityBaseProfileResponse;
import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalSecureEntrySummaryResponse(
        NationalCommunicationInstitutionalIdentityBaseProfileResponse identityBaseProfile,
        NationalCommunicationInstitutionalTrustAssessmentResponse assessment,
        List<NationalCommunicationInstitutionalAffiliationResponse> activeAffiliations,
        List<NationalCommunicationInstitutionalNominationResponse> activeNominations,
        List<NationalCommunicationInstitutionalEntryContextResponse> compatibleContexts,
        Instant generatedAt
) {
}