package com.tcc.pjb.backend.core.security.sigilo.api.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record SigiloAprovacaoResponse(
        UUID id,
        Long processoId,
        String status,
        LocalDateTime approvedAt,
        LocalDateTime expiresAt,
        String senhaTemporaria
) {
}
