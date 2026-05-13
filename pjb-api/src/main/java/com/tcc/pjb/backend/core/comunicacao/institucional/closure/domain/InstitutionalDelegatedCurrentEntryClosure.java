package com.tcc.pjb.backend.core.comunicacao.institucional.closure.domain;

import java.time.Instant;
import java.util.List;

public record InstitutionalDelegatedCurrentEntryClosure(
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
    public InstitutionalDelegatedCurrentEntryClosure {
        perfisDiretosPermitidos = perfisDiretosPermitidos == null ? List.of() : List.copyOf(perfisDiretosPermitidos);
        contextosDelegados = contextosDelegados == null ? List.of() : List.copyOf(contextosDelegados);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
