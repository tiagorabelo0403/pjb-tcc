package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel;

import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalObservabilityDashboardResponse(
        long totalEntregas,
        long totalPendentes,
        long totalDlq,
        long totalIntegracoesExternas,
        long totalIntegracoesAceitas,
        long totalIntegracoesFalha,
        long totalGatesBloqueando,
        long totalInboxPendentes,
        long totalSlaRisco,
        List<NationalCommunicationInstitutionalObservabilityBucketResponse> porCanal,
        List<NationalCommunicationInstitutionalObservabilityBucketResponse> porStatusEntrega,
        List<NationalCommunicationInstitutionalObservabilityBucketResponse> porDestinatario,
        Instant geradoEm
) {
}
