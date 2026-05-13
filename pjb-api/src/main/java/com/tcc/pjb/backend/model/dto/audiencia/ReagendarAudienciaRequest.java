package com.tcc.pjb.backend.model.dto.audiencia;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record ReagendarAudienciaRequest(
        @NotNull @Future LocalDateTime novaDataHora,
        @Size(max = 1000) String motivo
) {
}
