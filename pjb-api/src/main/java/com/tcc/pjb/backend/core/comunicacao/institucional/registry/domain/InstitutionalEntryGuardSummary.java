package com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalTrustAssessment;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntryContext;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalIdentityBaseProfile;
import java.time.Instant;
import java.util.List;

public record InstitutionalEntryGuardSummary(
        Long userId,
        String userName,
        InstitutionalIdentityBaseProfile identityBaseProfile,
        boolean identidadePessoalAutenticada,
        boolean vinculoInstitucionalValido,
        boolean contextoOperacionalAtivo,
        boolean autorizado,
        String affiliationId,
        String nominationId,
        InstitutionalTrustAssessment trustAssessment,
        List<InstitutionalEntryContext> contextosAtivos,
        List<String> trilhosAutenticacao,
        List<String> eixosAutorizacao,
        List<String> fundamentos,
        Instant evaluatedAt
) {
}
