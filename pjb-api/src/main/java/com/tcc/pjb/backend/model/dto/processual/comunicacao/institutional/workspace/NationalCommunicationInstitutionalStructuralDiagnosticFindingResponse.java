package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.workspace;

import java.util.List;

public record NationalCommunicationInstitutionalStructuralDiagnosticFindingResponse(
        String code,
        String severity,
        boolean blocking,
        String targetType,
        String targetId,
        String message,
        List<String> evidences
) {
}
