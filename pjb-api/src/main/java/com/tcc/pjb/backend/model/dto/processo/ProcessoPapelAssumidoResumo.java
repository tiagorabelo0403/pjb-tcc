package com.tcc.pjb.backend.model.dto.processo;

import java.time.LocalDateTime;

public record ProcessoPapelAssumidoResumo(
        String papelCode,
        String papelTitle,
        String nome,
        String documentoMascarado,
        String referenciaProfissional,
        LocalDateTime desde,
        String mensagem
) {
}
