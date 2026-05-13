package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel;

import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalPanelSummaryResponse(
        String unidadeCodigo,
        String unidadeSigla,
        String destinatarioKind,
        long totalExpedientes,
        long pendentesRecebimento,
        long pendentesCiencia,
        long pendentesCumprimento,
        long atrasados,
        List<String> caixasVisiveis,
        String horizontalDataPlaneKey,
        String rlsScopeKey,
        String coverageMode,
        boolean readOnly,
        Instant generatedAt
) {
}
