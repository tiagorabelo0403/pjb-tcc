package com.tcc.pjb.backend.model.dto.processual.comunicacao.flow;

import java.time.Instant;

public record NationalCommunicationDashboardResponse(
        long totalExpedidas,
        long totalEntregues,
        long totalPresumidas,
        long totalFrustradas,
        long totalPendentesOficial,
        long totalEvasoes,
        long totalEscalonadas,
        Instant geradoEm,
        String hashIntegridade) {
}
