package com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain;

import java.time.Instant;
import java.util.List;

public record InstitutionalOrgPanelSummary(
        String unidadeCodigo,
        String unidadeSigla,
        String destinatarioKind,
        long totalExpedientes,
        long pendentesRecebimento,
        long pendentesCiencia,
        long pendentesCumprimento,
        long atrasados,
        List<String> caixasVisiveis,
        Instant generatedAt
) {
}
