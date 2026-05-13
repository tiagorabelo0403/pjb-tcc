package com.tcc.pjb.backend.core.processo.unificado.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoMalhaOperacaoInstitucionalAggregate(
        Long processoId,
        String numeroProcesso,
        Long workItemId,
        Long inboxSnapshotId,
        String queueCode,
        String inboxKey,
        String status,
        String snapshotHash,
        List<String> fundamentos,
        Instant geradoEm
) {
    public ProcessoMalhaOperacaoInstitucionalAggregate {
        numeroProcesso = Objects.toString(numeroProcesso, "").trim();
        queueCode = Objects.toString(queueCode, "").trim();
        inboxKey = Objects.toString(inboxKey, "").trim();
        status = Objects.toString(status, "MATERIALIZADA").trim();
        snapshotHash = Objects.toString(snapshotHash, "").trim();
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
