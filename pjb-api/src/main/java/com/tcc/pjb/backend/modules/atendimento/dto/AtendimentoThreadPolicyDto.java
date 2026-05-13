package com.tcc.pjb.backend.modules.atendimento.dto;

import java.time.Instant;

public record AtendimentoThreadPolicyDto(
    Long threadId,
    Instant cidadaoSendDisabledUntil,
    Long updatedByUserId,
    Instant updatedAt
) {
}
