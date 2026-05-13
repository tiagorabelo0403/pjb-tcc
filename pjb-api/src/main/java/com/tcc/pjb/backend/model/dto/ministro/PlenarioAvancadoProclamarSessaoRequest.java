package com.tcc.pjb.backend.model.dto.ministro;

import jakarta.validation.constraints.NotBlank;

public record PlenarioAvancadoProclamarSessaoRequest(
        String ementa,
        @NotBlank String dispositivo,
        boolean gerarTemaVinculante,
        String tipoTema,
        String abrangencia,
        String fundamentosResumo
) {
}
