package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.procedural;

import java.util.List;

public record NationalCommunicationInstitutionalProcessDiagnosticFindingResponse(
        String code,
        String severity,
        boolean blocking,
        String profileCode,
        String message,
        List<String> evidences
) {
}
