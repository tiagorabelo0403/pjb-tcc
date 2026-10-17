package com.tcc.pjb.backend.model.dto.publico;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;

public record SessaoPublicaMediaDto(
        @JsonIgnore Long id,
        String tipo,
        String titulo,
        String urlPublica,
        String hashIntegridade,
        Instant createdAt
) {
}
