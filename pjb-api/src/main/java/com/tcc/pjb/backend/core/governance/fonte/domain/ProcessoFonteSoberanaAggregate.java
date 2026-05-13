package com.tcc.pjb.backend.core.governance.fonte.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoFonteSoberanaAggregate(Long processoId,
                                             String numeroProcesso,
                                             List<ProcessoFonteSoberanaRegistro> registros,
                                             int confiabilidadeMedia,
                                             boolean possuiConflito,
                                             boolean exigeRefresh,
                                             Instant geradoEm) {
    public ProcessoFonteSoberanaAggregate {
        numeroProcesso = Objects.toString(numeroProcesso, "").trim();
        registros = registros == null ? List.of() : List.copyOf(registros);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
