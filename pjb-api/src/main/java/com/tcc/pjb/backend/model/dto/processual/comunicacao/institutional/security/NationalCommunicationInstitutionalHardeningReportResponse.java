package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security;

import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalHardeningReportResponse(
        boolean aprovado,
        long totalUnidades,
        long totalUnidadesAtivas,
        long totalInboxPendentes,
        long totalGatesBloqueando,
        long totalDlq,
        long totalIntegracoesExternasComFalha,
        long totalEntregasEmAberto,
        List<String> canaisExternosCobertos,
        List<NationalCommunicationInstitutionalHardeningFindingResponse> findings,
        Instant geradoEm,
        String hashIntegridade
) {
}
