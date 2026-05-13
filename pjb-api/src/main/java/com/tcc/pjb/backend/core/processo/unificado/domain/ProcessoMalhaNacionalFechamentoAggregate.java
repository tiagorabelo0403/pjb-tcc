package com.tcc.pjb.backend.core.processo.unificado.domain;

import com.tcc.pjb.backend.core.processo.anomalia.domain.ProcessoAntifraudeOperacionalAggregate;
import com.tcc.pjb.backend.core.processo.distribuicao.domain.ProcessoDistribuicaoMalhaOrquestracaoAggregate;
import com.tcc.pjb.backend.core.processo.painel.domain.ProcessoPainelMalhaPapelAggregate;
import com.tcc.pjb.backend.core.processo.runtime.domain.ProcessoRuntimePreparationAggregate;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoMalhaNacionalFechamentoAggregate(Long processoId,
                                                       String numeroProcesso,
                                                       ProcessoRuntimePreparationAggregate runtime,
                                                       ProcessoDistribuicaoMalhaOrquestracaoAggregate distribuicao,
                                                       ProcessoMalhaObservabilidadeAggregate observabilidade,
                                                       ProcessoAntifraudeOperacionalAggregate antifraude,
                                                       ProcessoPainelMalhaPapelAggregate painelPapel,
                                                       List<String> fundamentos,
                                                       Instant geradoEm) {
    public ProcessoMalhaNacionalFechamentoAggregate {
        numeroProcesso = Objects.toString(numeroProcesso, "").trim();
        runtime = Objects.requireNonNull(runtime);
        distribuicao = Objects.requireNonNull(distribuicao);
        observabilidade = Objects.requireNonNull(observabilidade);
        antifraude = Objects.requireNonNull(antifraude);
        painelPapel = Objects.requireNonNull(painelPapel);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
