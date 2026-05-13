package com.tcc.pjb.backend.core.processo.painel.domain;

import java.time.Instant;
import java.util.List;

public record ProcessoPainelContextualAggregate(
        Long processoId,
        String numeroProcesso,
        String ramoDireito,
        String painelCodigo,
        String painelTitulo,
        String perfilCodigo,
        List<ProcessoPainelContextualWidget> widgets,
        List<ProcessoPainelConectorSaude> conectores,
        List<String> fundamentos,
        Instant geradoEm
) {
    public ProcessoPainelContextualAggregate {
        widgets = widgets == null ? List.of() : List.copyOf(widgets);
        conectores = conectores == null ? List.of() : List.copyOf(conectores);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
