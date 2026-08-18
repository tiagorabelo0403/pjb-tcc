package com.tcc.pjb.backend.core.processo.documental.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

public record ProcessoDocumentoVersao(
        String documentoId,
        int versao,
        String titulo,
        String estado,
        String sha256,
        @Schema(description = "Data/hora de criação da versão do documento processual", format = "date-time",
                example = "2026-06-01T10:00:00-03:00") String criadoEm,
        boolean custodioAtivo,
        boolean assinaturaExigida
) {
    public ProcessoDocumentoVersao {
        Objects.requireNonNull(documentoId);
        Objects.requireNonNull(titulo);
        Objects.requireNonNull(estado);
    }
}
