package com.tcc.pjb.backend.core.comunicacao.institucional.delivery.domain;

import java.time.Instant;
import java.util.Objects;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.enums.CanalComunicacaoInstitucional;
import com.tcc.pjb.backend.model.entity.enums.MotivoFalhaEntregaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.StatusTentativaEntregaInstitucional;

public record InstitutionalDeliveryAttempt(
        String attemptId,
        String jobId,
        String expedicaoUuid,
        int attemptNumber,
        CanalComunicacaoInstitucional channel,
        StatusTentativaEntregaInstitucional status,
        Instant startedAt,
        Instant endedAt,
        String providerReference,
        String providerStatus,
        MotivoFalhaEntregaInstitucional failureReason,
        boolean transientFailure,
        String detail,
        String hashIntegridade
) {
    public InstitutionalDeliveryAttempt {
        Objects.requireNonNull(attemptId, "attemptId");
        Objects.requireNonNull(jobId, "jobId");
        Objects.requireNonNull(expedicaoUuid, "expedicaoUuid");
        Objects.requireNonNull(channel, "channel");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(startedAt, "startedAt");
        Objects.requireNonNull(endedAt, "endedAt");
        hashIntegridade = hashIntegridade == null || hashIntegridade.isBlank()
                ? Hashes.sha256Hex(jobId + "|" + attemptNumber + "|" + channel.name() + "|" + status.name() + "|" + endedAt)
                : hashIntegridade;
    }
}
