package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance;

import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalOperationalProfileResponse;
import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalNominationResponse(
        String nominationId,
        String affiliationId,
        Long nominatedUserId,
        String nominatedUserName,
        String tipoUsuario,
        String accessLaneKind,
        String nominationRole,
        String funcaoOperacional,
        String processProfile,
        String unidadeCodigo,
        String caixaCodigo,
        List<String> capacidades,
        String trustFloor,
        String panelPreferencial,
        String status,
        Instant ativaDe,
        Instant ativaAte,
        boolean requerStepUpMfa,
        boolean requerCertificadoICP,
        boolean requerRedeInstitucional,
        boolean permiteUsoRemotoAutorizado,
        NationalCommunicationInstitutionalOperationalProfileResponse operationalProfile,
        Instant createdAt,
        Instant updatedAt
) {
}