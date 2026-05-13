package com.tcc.pjb.backend.model.dto.profile.operational;

import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;

public record ConciliacaoTermoAcordoRequest(
        @NotBlank String clausulas,
        @NotEmpty List<String> partes,
        @PositiveOrZero double valor
) {}
