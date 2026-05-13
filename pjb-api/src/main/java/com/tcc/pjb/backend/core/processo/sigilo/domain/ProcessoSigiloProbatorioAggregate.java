package com.tcc.pjb.backend.core.processo.sigilo.domain;

import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoSigiloProbatorioAggregate(
        Long processoId,
        String numeroProcesso,
        NivelSigilo nivelAtual,
        NivelSigilo nivelRecomendado,
        boolean exigeReclassificacao,
        long documentosAnalisados,
        long documentosCriticos,
        long provasCompartilhadas,
        List<ProcessoSigiloProbatorioItem> itens,
        List<String> alertas,
        List<String> fundamentos,
        Instant geradoEm
) {
    public ProcessoSigiloProbatorioAggregate {
        numeroProcesso = Objects.toString(numeroProcesso, "").trim();
        nivelAtual = nivelAtual == null ? NivelSigilo.PUBLICO : nivelAtual;
        nivelRecomendado = nivelRecomendado == null ? nivelAtual : nivelRecomendado;
        itens = itens == null ? List.of() : List.copyOf(itens);
        alertas = alertas == null ? List.of() : List.copyOf(alertas);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
