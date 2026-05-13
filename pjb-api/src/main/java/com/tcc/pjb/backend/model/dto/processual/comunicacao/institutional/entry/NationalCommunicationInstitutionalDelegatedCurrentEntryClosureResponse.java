package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry;

import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalDelegatedCurrentEntryClosureResponse(
        Long userId,
        String identityCode,
        boolean possuiAmbientePessoal,
        boolean possuiAmbienteInstitucional,
        boolean possuiPerfilDiretoAutorizado,
        boolean possuiContextoDelegadoAtivo,
        List<String> perfisDiretosPermitidos,
        List<String> contextosDelegados,
        List<String> fundamentos,
        Instant generatedAt
) {
}
