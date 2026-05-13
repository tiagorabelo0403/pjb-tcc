package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.procedural;

import java.util.List;

public record NationalCommunicationInstitutionalProcessSeparatorResponse(
        String code,
        String title,
        String accentColor,
        int ordem,
        boolean active,
        List<String> filtros,
        List<String> marcadores,
        List<String> fundamentos
) {
}
