package com.tcc.pjb.backend.model.dto.profile.operational;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record AdvogadoProrrogacaoPrazoLoteRequest(
        @NotEmpty List<Long> processoIds,
        @NotBlank String justificativa
) {}
