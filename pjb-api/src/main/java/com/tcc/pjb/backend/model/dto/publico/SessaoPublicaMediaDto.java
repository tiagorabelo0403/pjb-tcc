package com.tcc.pjb.backend.model.dto.publico;

import java.time.Instant;

public record SessaoPublicaMediaDto(
        Long id,
        String tipo,
        String titulo,
        String urlPublica,
        String hashIntegridade,
        Instant createdAt
) {
}
