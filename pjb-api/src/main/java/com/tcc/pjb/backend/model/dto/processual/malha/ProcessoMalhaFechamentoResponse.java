package com.tcc.pjb.backend.model.dto.processual.malha;

import java.time.Instant;
import java.util.List;

public record ProcessoMalhaFechamentoResponse(
        Long processoId,
        String numeroProcesso,
        String acaoDistribuicao,
        boolean distribuicaoBloqueada,
        boolean remessaManual,
        boolean redistribuicaoManual,
        int scoreAntifraude,
        String statusObservabilidade,
        ProcessoMalhaAtorResponse ator,
        ProcessoMalhaSigiloResponse sigilo,
        ProcessoMalhaPainelPapelResponse painelPapel,
        ProcessoMalhaRuntimeResponse runtime,
        List<String> fundamentos,
        Instant geradoEm
) {
    public ProcessoMalhaFechamentoResponse {
        numeroProcesso = numeroProcesso == null ? "" : numeroProcesso.trim();
        acaoDistribuicao = acaoDistribuicao == null ? "" : acaoDistribuicao.trim();
        statusObservabilidade = statusObservabilidade == null ? "NAO_INFORMADO" : statusObservabilidade.trim();
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
