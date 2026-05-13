package com.tcc.pjb.backend.model.dto.processual.malha;

import java.time.Instant;
import java.util.List;

public record ProcessoMalhaExecucaoAssistidaResponse(
        Long processoId,
        String numeroProcesso,
        String statusExecucao,
        String acaoRecomendada,
        ProcessoMalhaAtorResponse ator,
        ProcessoMalhaSigiloResponse sigilo,
        ProcessoMalhaRuntimeResponse runtime,
        ProcessoMalhaFechamentoResponse fechamento,
        ProcessoMalhaPainelPapelResponse painelSolicitado,
        List<ProcessoMalhaRotaTaticaItemResponse> rotaTatica,
        List<String> fundamentos,
        Instant geradoEm
) {
    public ProcessoMalhaExecucaoAssistidaResponse {
        numeroProcesso = numeroProcesso == null ? "" : numeroProcesso.trim();
        statusExecucao = statusExecucao == null ? "ASSISTIDA" : statusExecucao.trim();
        acaoRecomendada = acaoRecomendada == null ? "MANTER_MONITORAMENTO" : acaoRecomendada.trim();
        rotaTatica = rotaTatica == null ? List.of() : List.copyOf(rotaTatica);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
