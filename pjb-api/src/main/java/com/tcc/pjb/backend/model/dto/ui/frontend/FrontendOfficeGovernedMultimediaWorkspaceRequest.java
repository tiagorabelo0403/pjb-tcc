package com.tcc.pjb.backend.model.dto.ui.frontend;

import java.util.Map;

public record FrontendOfficeGovernedMultimediaWorkspaceRequest(
        String action,
        String actorLane,
        String pieceKind,
        Boolean preparingProtocolPackage,
        Boolean sigiloSensivel,
        Boolean tecnicoPericial,
        Map<String, Object> rawRequest
) {
}
