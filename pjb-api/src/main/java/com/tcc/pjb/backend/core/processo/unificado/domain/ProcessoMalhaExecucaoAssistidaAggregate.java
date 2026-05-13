package com.tcc.pjb.backend.core.processo.unificado.domain;

import com.tcc.pjb.backend.core.processo.painel.domain.ProcessoPainelRotaTaticaAggregate;
import com.tcc.pjb.backend.core.processo.runtime.domain.ProcessoRuntimePreparationAggregate;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoMalhaExecucaoAssistidaAggregate(Long processoId,
                                                      String numeroProcesso,
                                                      String statusExecucao,
                                                      String acaoRecomendada,
                                                      ProcessoRuntimePreparationAggregate runtime,
                                                      ProcessoMalhaNacionalFechamentoAggregate fechamento,
                                                      ProcessoPainelRotaTaticaAggregate rotaTatica,
                                                      List<String> fundamentos,
                                                      Instant geradoEm) {
    public ProcessoMalhaExecucaoAssistidaAggregate {
        numeroProcesso = Objects.toString(numeroProcesso, "").trim();
        statusExecucao = Objects.toString(statusExecucao, "ASSISTIDA").trim();
        acaoRecomendada = Objects.toString(acaoRecomendada, "MANTER_MONITORAMENTO").trim();
        runtime = Objects.requireNonNull(runtime);
        fechamento = Objects.requireNonNull(fechamento);
        rotaTatica = Objects.requireNonNull(rotaTatica);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
