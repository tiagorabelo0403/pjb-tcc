package com.tcc.pjb.backend.core.frontend.app.domain;

import java.util.List;

public record PjbFrontendOfficeProcessAccessView(
        Long processoId,
        String numeroProcesso,
        Long activeEquipeId,
        String mode,
        String actionType,
        boolean allowed,
        boolean visibleInWorkspace,
        boolean queueRequired,
        Long effectiveSignerUserId,
        String effectiveSignerNome,
        List<String> blockers,
        List<String> warnings
) {
}
