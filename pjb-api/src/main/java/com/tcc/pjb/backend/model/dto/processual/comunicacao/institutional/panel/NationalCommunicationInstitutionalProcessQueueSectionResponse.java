package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel;

import java.util.List;

public record NationalCommunicationInstitutionalProcessQueueSectionResponse(
        String code,
        String title,
        String accentColor,
        int ordem,
        List<String> filtros,
        List<String> indicadores,
        List<String> ordenacoes
) {
}
