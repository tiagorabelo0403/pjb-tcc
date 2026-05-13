package com.tcc.pjb.backend.model.dto.processual.substituicao.nacional.cockpit;

public record PjbSubstituicaoNacionalOperacionalResumoResponse(
        int totalEventos,
        int totalProbes,
        int probesAprovadas,
        int probesBloqueadas,
        int probesSimuladas,
        int totalLotes,
        int lotesReconciliados,
        int lotesBloqueados,
        int divergenciasMigracao,
        int totalCursores,
        int totalItensComunicacao,
        int itensCorrelacionados,
        int itensDeduplicados,
        int itensReprocessaveis
) {
}
