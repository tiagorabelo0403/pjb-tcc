package com.tcc.pjb.backend.model.dto.secretariat;

import com.tcc.pjb.backend.model.entity.enums.TipoUnidadeInstitucional;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record VistaInstitucionalRequest(
        @NotNull TipoUnidadeInstitucional tipoInstituicaoAlvo,
        @NotNull @Min(1) Integer prazoBaseDias
) {
}
