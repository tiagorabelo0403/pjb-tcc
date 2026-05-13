package com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain;

import java.util.List;
import java.util.Objects;

public record InstitutionalTrustMatrixEntry(
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
    public InstitutionalTrustMatrixEntry {
        Objects.requireNonNull(codigo);
        Objects.requireNonNull(nomeExibicao);
        fatoresObrigatorios = fatoresObrigatorios == null ? List.of() : List.copyOf(fatoresObrigatorios);
        fatoresComplementares = fatoresComplementares == null ? List.of() : List.copyOf(fatoresComplementares);
        capacidadesPermitidas = capacidadesPermitidas == null ? List.of() : List.copyOf(capacidadesPermitidas);
        restricoes = restricoes == null ? List.of() : List.copyOf(restricoes);
        guardRails = guardRails == null ? List.of() : List.copyOf(guardRails);
        rotasIniciais = rotasIniciais == null ? List.of() : List.copyOf(rotasIniciais);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
