package com.tcc.pjb.backend.model.dto.ministro;

import jakarta.validation.constraints.NotBlank;

public record PlenarioAvancadoRegistrarVotoRequest(
        @NotBlank String opcaoVoto,
        String fundamentacaoResumo,
        String ressalva
) {
}
