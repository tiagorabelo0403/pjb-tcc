package com.tcc.pjb.backend.core.processo.painel.domain;

import java.time.Instant;
import java.util.List;

public record ProcessoPainelFonteOficialAggregate(
        Long processoId,
        String numeroProcesso,
        String ramoDireito,
        List<ProcessoPainelFonteOficialItem> itens,
        List<String> garantias,
        Instant geradoEm
) {
    public ProcessoPainelFonteOficialAggregate {
        numeroProcesso = numeroProcesso == null ? "" : numeroProcesso;
        ramoDireito = ramoDireito == null ? "NAO_INFORMADO" : ramoDireito;
        itens = itens == null ? List.of() : List.copyOf(itens);
        garantias = garantias == null ? List.of() : List.copyOf(garantias);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
