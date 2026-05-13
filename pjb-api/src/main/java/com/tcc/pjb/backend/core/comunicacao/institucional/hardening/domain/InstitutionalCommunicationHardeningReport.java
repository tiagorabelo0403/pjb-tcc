package com.tcc.pjb.backend.core.comunicacao.institucional.hardening.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import com.tcc.pjb.backend.core.util.PayloadMaps;

public record InstitutionalCommunicationHardeningReport(
        boolean aprovado,
        long totalUnidades,
        long totalUnidadesAtivas,
        long totalInboxPendentes,
        long totalGatesBloqueando,
        long totalDlq,
        long totalIntegracoesExternasComFalha,
        long totalEntregasEmAberto,
        List<String> canaisExternosCobertos,
        List<InstitutionalHardeningFinding> findings,
        Instant geradoEm,
        String hashIntegridade
) {
    public InstitutionalCommunicationHardeningReport {
        canaisExternosCobertos = PayloadMaps.copyDistinctStrings(canaisExternosCobertos);
        findings = PayloadMaps.copyListDistinct(findings);
        geradoEm = Objects.requireNonNull(geradoEm, "geradoEm");
        hashIntegridade = Objects.requireNonNull(hashIntegridade, "hashIntegridade");
    }
}
