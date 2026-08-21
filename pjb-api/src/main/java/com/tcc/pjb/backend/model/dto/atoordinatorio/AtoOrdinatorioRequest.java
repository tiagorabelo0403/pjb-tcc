package com.tcc.pjb.backend.model.dto.atoordinatorio;

import com.tcc.pjb.backend.model.entity.enums.TipoAtoOrdinatorio;
import jakarta.validation.constraints.NotNull;

public record AtoOrdinatorioRequest(
        @NotNull Long processoId,
        @NotNull TipoAtoOrdinatorio tipo,
        String complemento
) {
}
