package com.tcc.pjb.backend.core.events;

import java.time.Instant;
import java.util.Objects;

public record PessoaApreendidaEvent(
        String custodiaId,
        String pessoaDocumento,
        String tipoApreensao,
        String delegaciaOrigem,
        String comarca,
        String uf,
        Instant apreendidaEm,
        Instant prazoAudienciaCustodia
) {
    public PessoaApreendidaEvent {
        Objects.requireNonNull(custodiaId, "custodiaId");
        Objects.requireNonNull(tipoApreensao, "tipoApreensao");
        apreendidaEm = apreendidaEm == null ? Instant.now() : apreendidaEm;
        if (prazoAudienciaCustodia == null) {
            prazoAudienciaCustodia = apreendidaEm.plusSeconds(86400);
        }
    }
}
