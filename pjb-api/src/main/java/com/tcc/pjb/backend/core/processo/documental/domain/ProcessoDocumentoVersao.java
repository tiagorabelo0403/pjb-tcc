package com.tcc.pjb.backend.core.processo.documental.domain;

import java.util.Objects;

public record ProcessoDocumentoVersao(
        String documentoId,
        int versao,
        String titulo,
        String estado,
        String sha256,
        String criadoEm,
        boolean custodioAtivo,
        boolean assinaturaExigida
) {
    public ProcessoDocumentoVersao {
        Objects.requireNonNull(documentoId);
        Objects.requireNonNull(titulo);
        Objects.requireNonNull(estado);
    }
}
