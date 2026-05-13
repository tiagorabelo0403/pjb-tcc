package com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain;

import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntryLandingPanel;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalEntryMode;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalSecurityFactor;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalTrustLevel;
import java.time.Instant;
import java.util.List;
import java.util.Set;

public record InstitutionalTrustAssessment(
        Long userId,
        String userName,
        InstitutionalEntryMode entryMode,
        String affiliationId,
        String nominationId,
        InstitutionalTrustLevel trustLevel,
        Set<InstitutionalSecurityFactor> factors,
        boolean trustedInstitutionalNetwork,
        boolean managedInstitutionalLogin,
        boolean remoteCertificateAuthorizationActive,
        boolean certificadoPermitidoNaSessao,
        boolean mfaAtivo,
        boolean autorizado,
        InstitutionalEntryLandingPanel panelPreferencial,
        List<String> reasons,
        Instant evaluatedAt
) {
}
