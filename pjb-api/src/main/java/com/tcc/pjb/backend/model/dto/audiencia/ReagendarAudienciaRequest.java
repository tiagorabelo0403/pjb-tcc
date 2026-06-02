package com.tcc.pjb.backend.model.dto.audiencia;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record ReagendarAudienciaRequest(
        @NotNull @Future @Schema(description = "Nova data e hora da audiência reagendada (deve ser futura)", format = "date-time",
                example = "2026-07-20T14:00:00-03:00") LocalDateTime novaDataHora,
        @Size(max = 1000) String motivo
) {
}
