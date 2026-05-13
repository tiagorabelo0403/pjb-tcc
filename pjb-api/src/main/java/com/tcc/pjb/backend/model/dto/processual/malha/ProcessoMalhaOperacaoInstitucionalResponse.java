package com.tcc.pjb.backend.model.dto.processual.malha;

import java.time.Instant;
import java.util.List;

public record ProcessoMalhaOperacaoInstitucionalResponse(
        Long processoId,
        String numeroProcesso,
        Long workItemId,
        Long inboxSnapshotId,
        String queueCode,
        String inboxKey,
        String status,
        String snapshotHash,
        ProcessoMalhaAtorResponse ator,
        ProcessoMalhaSigiloResponse sigilo,
        List<String> fundamentos,
        Instant geradoEm
) {
    public ProcessoMalhaOperacaoInstitucionalResponse {
        numeroProcesso = numeroProcesso == null ? "" : numeroProcesso.trim();
        queueCode = queueCode == null ? "" : queueCode.trim();
        inboxKey = inboxKey == null ? "" : inboxKey.trim();
        status = status == null ? "MATERIALIZADA" : status.trim();
        snapshotHash = snapshotHash == null ? "" : snapshotHash.trim();
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
