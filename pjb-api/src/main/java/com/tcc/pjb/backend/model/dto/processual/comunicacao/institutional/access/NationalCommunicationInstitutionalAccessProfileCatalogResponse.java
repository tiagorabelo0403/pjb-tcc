package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.access;

import java.util.List;

public record NationalCommunicationInstitutionalAccessProfileCatalogResponse(
        String codigo,
        String nomeExibicao,
        String entryMode,
        String nominationRole,
        String processProfile,
        String panel,
        String trustFloor,
        List<String> capacidadesPadrao,
        List<String> restricoes,
        List<String> fundamentos
) {
}
