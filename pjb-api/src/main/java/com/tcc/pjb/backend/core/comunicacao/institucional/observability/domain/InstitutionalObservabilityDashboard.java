package com.tcc.pjb.backend.core.comunicacao.institucional.observability.domain;

import java.time.Instant;
import java.util.List;

public record InstitutionalObservabilityDashboard(
        long totalEntregas,
        long totalPendentes,
        long totalDlq,
        long totalIntegracoesExternas,
        long totalIntegracoesAceitas,
        long totalIntegracoesFalha,
        long totalGatesBloqueando,
        long totalInboxPendentes,
        long totalSlaRisco,
        List<InstitutionalObservabilityBucket> porCanal,
        List<InstitutionalObservabilityBucket> porStatusEntrega,
        List<InstitutionalObservabilityBucket> porDestinatario,
        Instant geradoEm
) {
}
