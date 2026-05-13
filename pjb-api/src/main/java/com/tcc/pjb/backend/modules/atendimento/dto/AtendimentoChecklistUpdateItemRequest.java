package com.tcc.pjb.backend.modules.atendimento.dto;

import java.time.Instant;

public record AtendimentoChecklistUpdateItemRequest(
    String title,
    String note,
    Instant dueAt,
    Long documentoId
) {
}
