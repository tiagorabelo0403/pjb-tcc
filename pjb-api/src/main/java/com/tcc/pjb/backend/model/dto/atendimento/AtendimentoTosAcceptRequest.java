package com.tcc.pjb.backend.model.dto.atendimento;

import jakarta.validation.constraints.Min;

public record AtendimentoTosAcceptRequest(
        @Min(1) int version
) {
}
