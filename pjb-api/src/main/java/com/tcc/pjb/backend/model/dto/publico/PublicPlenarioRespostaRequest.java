package com.tcc.pjb.backend.model.dto.publico;

import jakarta.validation.constraints.NotBlank;

public record PublicPlenarioRespostaRequest(
        @NotBlank String respostaPublica,
        boolean visivelPublicamente
) {
}
