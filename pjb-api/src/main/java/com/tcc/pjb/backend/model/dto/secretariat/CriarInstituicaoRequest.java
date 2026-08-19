package com.tcc.pjb.backend.model.dto.secretariat;

import com.tcc.pjb.backend.model.entity.enums.TipoInstituicao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CriarInstituicaoRequest(@NotNull TipoInstituicao tipo, @NotBlank String nome, String sigla) {
}
