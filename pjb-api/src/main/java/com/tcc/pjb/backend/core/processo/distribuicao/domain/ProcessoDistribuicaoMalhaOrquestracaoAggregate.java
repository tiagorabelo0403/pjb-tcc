package com.tcc.pjb.backend.core.processo.distribuicao.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoDistribuicaoMalhaOrquestracaoAggregate(
        Long processoId,
        String numeroProcesso,
        String statusOrquestracao,
        String acaoExecutada,
        boolean bloqueada,
        boolean remessaManual,
        boolean redistribuicaoManual,
        Long workItemId,
        String filaOperacional,
        String inboxOperacional,
        String unidadeDestino,
        int prioridade,
        boolean timelineMaterializada,
        boolean anomaliaEscalada,
        List<String> fundamentos,
        Instant geradoEm
) {
    public ProcessoDistribuicaoMalhaOrquestracaoAggregate {
        numeroProcesso = Objects.toString(numeroProcesso, "").trim();
        statusOrquestracao = Objects.toString(statusOrquestracao, "PENDENTE").trim();
        acaoExecutada = Objects.toString(acaoExecutada, "SEM_ACAO").trim();
        filaOperacional = Objects.toString(filaOperacional, "").trim();
        inboxOperacional = Objects.toString(inboxOperacional, "").trim();
        unidadeDestino = Objects.toString(unidadeDestino, "").trim();
        prioridade = Math.max(0, prioridade);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
