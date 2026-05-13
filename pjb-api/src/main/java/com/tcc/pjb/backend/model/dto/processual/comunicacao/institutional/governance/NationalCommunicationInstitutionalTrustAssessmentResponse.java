package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance;

import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalTrustAssessmentResponse(
        Long userId,
        String userName,
        String entryMode,
        String affiliationId,
        String nominationId,
        String trustLevel,
        List<String> factors,
        boolean trustedInstitutionalNetwork,
        boolean managedInstitutionalLogin,
        boolean remoteCertificateAuthorizationActive,
        boolean certificadoPermitidoNaSessao,
        boolean mfaAtivo,
        boolean autorizado,
        String panelPreferencial,
        List<String> reasons,
        Instant evaluatedAt
) {
}
