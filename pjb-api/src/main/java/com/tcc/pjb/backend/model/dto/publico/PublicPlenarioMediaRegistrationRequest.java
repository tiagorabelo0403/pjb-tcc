package com.tcc.pjb.backend.model.dto.publico;

import jakarta.validation.constraints.NotBlank;

public record PublicPlenarioMediaRegistrationRequest(
        @NotBlank String tipo,
        @NotBlank String titulo,
        @NotBlank String urlPublica,
        String hashIntegridade,
        Integer ordemExibicao,
        boolean publico
) {
}
