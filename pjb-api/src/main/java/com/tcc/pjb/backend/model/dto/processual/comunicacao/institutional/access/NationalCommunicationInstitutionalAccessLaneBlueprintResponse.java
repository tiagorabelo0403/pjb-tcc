package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.access;

import java.util.List;

public record NationalCommunicationInstitutionalAccessLaneBlueprintResponse(
        String laneKind,
        String codigo,
        String nomeExibicao,
        String nominationRole,
        String funcaoOperacional,
        String processProfile,
        String panel,
        String trustFloor,
        List<String> capacidadesPadrao,
        boolean requerStepUpMfa,
        boolean requerCertificadoICP,
        boolean requerRedeInstitucional,
        boolean permiteUsoRemotoAutorizado,
        List<String> restricoes,
        List<String> fundamentos
) {
}
