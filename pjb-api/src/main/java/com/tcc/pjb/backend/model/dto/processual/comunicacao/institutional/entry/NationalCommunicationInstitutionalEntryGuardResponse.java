package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry;

import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalTrustAssessmentResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalIdentityBaseProfileResponse;
import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalEntryGuardResponse(
        Long userId,
        String userName,
        NationalCommunicationInstitutionalIdentityBaseProfileResponse identityBaseProfile,
        boolean identidadePessoalAutenticada,
        boolean vinculoInstitucionalValido,
        boolean contextoOperacionalAtivo,
        boolean autorizado,
        String affiliationId,
        String nominationId,
        NationalCommunicationInstitutionalTrustAssessmentResponse trustAssessment,
        List<NationalCommunicationInstitutionalEntryContextResponse> contextosAtivos,
        List<String> trilhosAutenticacao,
        List<String> eixosAutorizacao,
        List<String> fundamentos,
        Instant evaluatedAt
) {
}