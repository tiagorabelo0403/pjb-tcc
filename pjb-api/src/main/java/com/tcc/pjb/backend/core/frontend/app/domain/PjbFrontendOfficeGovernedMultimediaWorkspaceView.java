package com.tcc.pjb.backend.core.frontend.app.domain;

import java.util.List;
import java.util.Map;

public record PjbFrontendOfficeGovernedMultimediaWorkspaceView(
        Long processoId,
        String action,
        String actorLane,
        String pieceKind,
        boolean accessAllowed,
        boolean queueRequired,
        String workspaceMode,
        Long activeEquipeId,
        Long effectiveSignerUserId,
        String effectiveSignerNome,
        List<String> accessBlockers,
        List<String> accessWarnings,
        String nextAction,
        String pieceProfile,
        boolean multimediaEnabled,
        Map<String, Object> workspace
) {
}
