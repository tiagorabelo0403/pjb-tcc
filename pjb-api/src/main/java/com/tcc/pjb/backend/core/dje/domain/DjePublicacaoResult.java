package com.tcc.pjb.backend.core.dje.domain;

import java.time.LocalDate;

public record DjePublicacaoResult(
        Long djeId,
        LocalDate dataDisponibilizacao,
        LocalDate dataPublicacao,
        LocalDate prazoComecaEm
) {
}
