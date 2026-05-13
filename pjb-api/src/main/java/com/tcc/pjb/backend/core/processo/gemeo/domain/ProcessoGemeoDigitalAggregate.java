package com.tcc.pjb.backend.core.processo.gemeo.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoGemeoDigitalAggregate(Long processoId,
                                            String numeroProcesso,
                                            ProcessoGemeoDigitalEstado estadoAtual,
                                            List<String> estadosPossiveis,
                                            List<ProcessoGemeoDigitalRisco> riscos,
                                            String gargaloProvavel,
                                            String proximoAtoRecomendado,
                                            int custoOperacionalEstimado,
                                            Instant geradoEm) {
    public ProcessoGemeoDigitalAggregate {
        numeroProcesso = Objects.toString(numeroProcesso, "").trim();
        estadoAtual = estadoAtual == null ? ProcessoGemeoDigitalEstado.ESTAVEL : estadoAtual;
        estadosPossiveis = estadosPossiveis == null ? List.of() : List.copyOf(estadosPossiveis);
        riscos = riscos == null ? List.of() : List.copyOf(riscos);
        gargaloProvavel = Objects.toString(gargaloProvavel, "").trim();
        proximoAtoRecomendado = Objects.toString(proximoAtoRecomendado, "").trim();
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
