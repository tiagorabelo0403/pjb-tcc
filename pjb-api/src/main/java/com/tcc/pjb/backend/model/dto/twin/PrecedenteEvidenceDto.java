package com.tcc.pjb.backend.model.dto.twin;

import java.time.LocalDate;

public record PrecedenteEvidenceDto(
        Long id,
        String fonte,
        String tipo,
        String identificador,
        String titulo,
        String urlReferencia,
        LocalDate dataPublicacao
) {
}
