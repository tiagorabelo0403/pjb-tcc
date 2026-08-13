package com.tcc.pjb.backend.model.dto.secretariat;

import com.tcc.pjb.backend.model.entity.enums.TipoUnidadeInstitucional;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CriarUnidadeInstituicaoRequest(
        @NotNull Long instituicaoId,
        @NotBlank String nome,
        @NotNull TipoUnidadeInstitucional tipo,
        @NotBlank String comarca,
        @NotBlank String uf
) {
}
