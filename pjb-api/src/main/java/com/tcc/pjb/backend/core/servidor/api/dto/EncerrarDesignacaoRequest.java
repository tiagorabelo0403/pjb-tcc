package com.tcc.pjb.backend.core.servidor.api.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record EncerrarDesignacaoRequest(
        @NotNull LocalDate dataFim
) {
}
