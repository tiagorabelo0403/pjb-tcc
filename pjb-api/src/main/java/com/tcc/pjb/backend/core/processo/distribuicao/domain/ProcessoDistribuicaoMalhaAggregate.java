package com.tcc.pjb.backend.core.processo.distribuicao.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoDistribuicaoMalhaAggregate(
        Long processoId,
        String numeroProcesso,
        String acaoPrimaria,
        String filaSugerida,
        String inboxSugerida,
        String destinoTribunal,
        String destinoUnidade,
        int prioridade,
        boolean travaDistribuicao,
        boolean exigeRemessa,
        boolean exigeReuniao,
        boolean exigeSigiloReforcado,
        List<ProcessoDistribuicaoMalhaMotivo> motivos,
        List<String> fundamentos,
        Instant geradoEm
) {
    public ProcessoDistribuicaoMalhaAggregate {
        numeroProcesso = Objects.toString(numeroProcesso, "").trim();
        acaoPrimaria = Objects.toString(acaoPrimaria, "").trim();
        filaSugerida = Objects.toString(filaSugerida, "").trim();
        inboxSugerida = Objects.toString(inboxSugerida, "").trim();
        destinoTribunal = Objects.toString(destinoTribunal, "").trim();
        destinoUnidade = Objects.toString(destinoUnidade, "").trim();
        prioridade = Math.max(1, Math.min(99, prioridade));
        motivos = motivos == null ? List.of() : List.copyOf(motivos);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
