package com.tcc.pjb.backend.core.processo.plantao.domain;

import java.time.Instant;
import java.util.List;

public record ProcessoPlantaoSubstituicaoAggregate(
        Long processoId,
        String numeroProcesso,
        String unidadeCodigo,
        String regimeAtivo,
        boolean plantaoAtivo,
        boolean substituicaoAtiva,
        boolean delegacaoAtiva,
        boolean titularObrigatorio,
        String responsavelAtual,
        List<ProcessoPlantaoRegra> regrasAtivas,
        List<ProcessoResponsabilidadeOperacional> responsabilidades,
        List<String> escalonamento,
        List<String> fundamentos,
        List<String> alertas,
        Instant geradoEm
) {
    public ProcessoPlantaoSubstituicaoAggregate {
        numeroProcesso = numeroProcesso == null ? "" : numeroProcesso;
        unidadeCodigo = unidadeCodigo == null ? "" : unidadeCodigo;
        regimeAtivo = regimeAtivo == null || regimeAtivo.isBlank() ? "ROTINA" : regimeAtivo;
        responsavelAtual = responsavelAtual == null ? "" : responsavelAtual;
        regrasAtivas = regrasAtivas == null ? List.of() : List.copyOf(regrasAtivas);
        responsabilidades = responsabilidades == null ? List.of() : List.copyOf(responsabilidades);
        escalonamento = escalonamento == null ? List.of() : List.copyOf(escalonamento);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        alertas = alertas == null ? List.of() : List.copyOf(alertas);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
