package com.tcc.pjb.backend.model.dto;

import com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AnexoDeclarado(
        @NotBlank String nomeArquivo,
        @NotNull TipoDocumento tipo
) {}
