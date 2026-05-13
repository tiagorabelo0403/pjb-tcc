package com.tcc.pjb.backend.model.dto.publico;

import jakarta.validation.constraints.NotBlank;

public record PublicPlenarioEsclarecimentoRequest(
        @NotBlank String resumoDuvida,
        boolean visivelPublicamente
) {
}
