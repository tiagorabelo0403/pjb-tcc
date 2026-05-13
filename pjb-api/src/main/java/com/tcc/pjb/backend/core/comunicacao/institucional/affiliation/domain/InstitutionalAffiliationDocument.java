package com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain;

import java.util.Objects;

public record InstitutionalAffiliationDocument(
        String codigo,
        String nome,
        String tipo,
        String referenciaExterna,
        String hashDocumento,
        boolean obrigatorio,
        boolean validado
) {
    public InstitutionalAffiliationDocument {
        Objects.requireNonNull(codigo);
        Objects.requireNonNull(nome);
        Objects.requireNonNull(tipo);
        referenciaExterna = referenciaExterna == null ? "" : referenciaExterna.trim();
        hashDocumento = hashDocumento == null ? "" : hashDocumento.trim();
    }
}
