package com.tcc.pjb.backend.modules.acordo.api;

import com.tcc.pjb.backend.modules.acordo.domain.AcordoAuditoriaEvento;
import java.time.Instant;
import java.util.Map;

public record AcordoAuditEntry(
        Long sessaoId,
        Long usuarioId,
        AcordoAuditoriaEvento evento,
        Map<String, Object> detalhes,
        String ipHash,
        String userAgentHash,
        Instant createdAt
) {
}
