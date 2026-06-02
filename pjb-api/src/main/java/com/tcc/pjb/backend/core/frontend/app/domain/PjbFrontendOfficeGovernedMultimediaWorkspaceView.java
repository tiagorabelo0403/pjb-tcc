package com.tcc.pjb.backend.core.frontend.app.domain;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

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
        @Schema(description = "Estado acumulado do workspace de midia governado — dinamico por sessao e tipo de peticao", implementation = Object.class)
        @Size(max = 50)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> workspace
) {
}

