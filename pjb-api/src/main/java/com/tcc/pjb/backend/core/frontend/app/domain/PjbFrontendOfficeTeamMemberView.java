package com.tcc.pjb.backend.core.frontend.app.domain;

import java.time.Instant;

public record PjbFrontendOfficeTeamMemberView(
        Long userId,
        String nome,
        String email,
        String registroProfissional,
        String papelEquipe,
        String cargo,
        boolean patrono,
        boolean fundador,
        boolean currentUser,
        boolean currentWorkspaceSelected,
        boolean online,
        Instant lastSeenAt,
        Integer workspacePriority,
        String affiliationType
) {
}
