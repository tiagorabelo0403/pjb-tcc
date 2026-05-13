package com.tcc.pjb.backend.model.dto.ministro;

import java.time.Instant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MinistroPautaRequest(
        @NotNull Instant dataSessao,
        @NotBlank String orgao
) {}
