package com.tcc.pjb.backend.modules.atendimento.dto;

import java.time.Instant;
import jakarta.validation.constraints.NotBlank;

public record AtendimentoChecklistCreateItemRequest(
    String kind,
    @NotBlank String title,
    String note,
    Instant dueAt,
    Long documentoId
) {
}
