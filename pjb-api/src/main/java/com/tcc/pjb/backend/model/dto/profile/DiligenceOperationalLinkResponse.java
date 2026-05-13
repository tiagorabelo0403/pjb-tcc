package com.tcc.pjb.backend.model.dto.profile;

import java.time.Instant;

public record DiligenceOperationalLinkResponse(
        boolean vinculada,
        String canal,
        String diligenciaReferencia,
        Long workItemId,
        Long processoId,
        String processoNumero,
        String templateCode,
        String workItemType,
        String workItemStatus,
        String assignedRole,
        String assignedUser,
        Instant dueAt,
        String uf,
        String comarca
) {
}
