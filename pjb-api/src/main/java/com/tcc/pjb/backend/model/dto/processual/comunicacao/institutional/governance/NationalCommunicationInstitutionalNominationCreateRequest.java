package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance;

import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalNominationCreateRequest(
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
        Instant ativaDe,
        Instant ativaAte,
        Boolean requerStepUpMfa,
        Boolean requerCertificadoICP,
        Boolean requerRedeInstitucional,
        Boolean permiteUsoRemotoAutorizado
) {
}
