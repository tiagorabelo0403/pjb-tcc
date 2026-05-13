package com.tcc.pjb.backend.core.comunicacao.institucional.delivery.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.enums.CanalComunicacaoInstitucional;
import com.tcc.pjb.backend.model.entity.enums.MotivoFalhaEntregaInstitucional;

public record InstitutionalDeadLetterEntry(
        String entryId,
        String jobId,
        String expedicaoUuid,
        Long processoId,
        String processoNumero,
        String unidadeCodigo,
        String caixaCodigo,
        CanalComunicacaoInstitucional channel,
        MotivoFalhaEntregaInstitucional reason,
        int attempts,
        String detail,
        List<String> justificativas,
        Instant createdAt,
        String hashIntegridade
) {
    public InstitutionalDeadLetterEntry {
        Objects.requireNonNull(entryId, "entryId");
        Objects.requireNonNull(jobId, "jobId");
        Objects.requireNonNull(expedicaoUuid, "expedicaoUuid");
        Objects.requireNonNull(channel, "channel");
        Objects.requireNonNull(createdAt, "createdAt");
        justificativas = List.copyOf(justificativas == null ? List.of() : justificativas);
        hashIntegridade = hashIntegridade == null || hashIntegridade.isBlank()
                ? Hashes.sha256Hex(jobId + "|DLQ|" + channel.name() + "|" + createdAt)
                : hashIntegridade;
    }
}
