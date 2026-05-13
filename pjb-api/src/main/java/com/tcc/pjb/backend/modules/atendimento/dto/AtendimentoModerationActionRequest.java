package com.tcc.pjb.backend.modules.atendimento.dto;

import jakarta.validation.constraints.NotBlank;

public record AtendimentoModerationActionRequest(
    @NotBlank String reason,
    String note
) {
}
