package com.tcc.pjb.backend.core.processo.cooperacao.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoCooperacaoInstitucionalAggregate(Long processoId,
                                                       String numeroProcesso,
                                                       List<ProcessoCooperacaoInstitucionalItem> itens,
                                                       boolean exigeRetornoExterno,
                                                       boolean exigeCooperacaoSigilosa,
                                                       List<String> fundamentos,
                                                       Instant geradoEm) {
    public ProcessoCooperacaoInstitucionalAggregate {
        numeroProcesso = Objects.toString(numeroProcesso, "").trim();
        itens = itens == null ? List.of() : List.copyOf(itens);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
