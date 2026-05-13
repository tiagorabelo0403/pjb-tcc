package com.tcc.pjb.backend.model.dto.processual.substituicao.tribunal;

import java.time.Instant;
import java.util.List;

public record PjbSubstituicaoTribunalReconciliacaoResponse(
        String tribunalCodigo,
        String tribunalNome,
        int totalExecucoes,
        int totalEventos,
        int totalProbes,
        int totalLotes,
        int totalCursores,
        int totalItens,
        int totalCorrelacionados,
        int totalDeduplicados,
        int totalReprocessaveis,
        int totalDivergencias,
        String verdict,
        List<String> bloqueadores,
        PjbSubstituicaoTribunalEvidenciaExportavelResponse evidenciaExportavel,
        Instant geradoEm
) {
    public PjbSubstituicaoTribunalReconciliacaoResponse {
        bloqueadores = bloqueadores == null ? List.of() : List.copyOf(bloqueadores);
    }
}
