package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance;

import java.util.List;

public record NationalCommunicationInstitutionalTrustMatrixEntryResponse(
        String codigo,
        String escopo,
        String nomeExibicao,
        String entryMode,
        String laneKind,
        String nominationRole,
        String processProfile,
        String panel,
        String trustFloor,
        List<String> fatoresObrigatorios,
        List<String> fatoresComplementares,
        List<String> capacidadesPermitidas,
        List<String> restricoes,
        List<String> guardRails,
        List<String> rotasIniciais,
        List<String> fundamentos
) {
}
