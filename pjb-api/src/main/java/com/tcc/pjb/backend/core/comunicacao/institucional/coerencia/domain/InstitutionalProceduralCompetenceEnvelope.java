package com.tcc.pjb.backend.core.comunicacao.institucional.coerencia.domain;

import java.util.List;
import java.util.Objects;

public record InstitutionalProceduralCompetenceEnvelope(
        String eixoMaterial,
        String eixoProcedimental,
        String eixoFasico,
        String eixoAtuacao,
        boolean exigeAssinaturaForte,
        boolean exigeSegregacaoTitular,
        boolean bloqueiaAtosPosArquivamento,
        List<String> fundamentos
) {
    public InstitutionalProceduralCompetenceEnvelope {
        Objects.requireNonNull(eixoMaterial);
        Objects.requireNonNull(eixoProcedimental);
        Objects.requireNonNull(eixoFasico);
        Objects.requireNonNull(eixoAtuacao);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
