package com.tcc.pjb.backend.core.comunicacao.institucional.delivery.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.enums.CanalComunicacaoInstitucional;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.MotivoFalhaEntregaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.PapelProcessualInstitucional;
import com.tcc.pjb.backend.model.entity.enums.StatusEntregaInstitucional;

public record InstitutionalDeliveryJob(
        String jobId,
        String expedicaoUuid,
        Long processoId,
        String processoNumero,
        String unidadeCodigo,
        String caixaCodigo,
        DestinatarioInstitucionalKind destinatarioKind,
        PapelProcessualInstitucional papelProcessual,
        List<CanalComunicacaoInstitucional> channelChain,
        int currentChannelIndex,
        StatusEntregaInstitucional status,
        int attemptCount,
        int maxAttempts,
        Instant createdAt,
        Instant updatedAt,
        Instant nextAttemptAt,
        Instant lastAttemptAt,
        Instant terminalAt,
        String correlationKey,
        String providerReference,
        MotivoFalhaEntregaInstitucional lastFailureReason,
        String lastError,
        List<String> justificativas,
        String hashIntegridade
) {
    public InstitutionalDeliveryJob {
        Objects.requireNonNull(jobId, "jobId");
        Objects.requireNonNull(expedicaoUuid, "expedicaoUuid");
        Objects.requireNonNull(unidadeCodigo, "unidadeCodigo");
        Objects.requireNonNull(caixaCodigo, "caixaCodigo");
        Objects.requireNonNull(destinatarioKind, "destinatarioKind");
        Objects.requireNonNull(papelProcessual, "papelProcessual");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
        Objects.requireNonNull(nextAttemptAt, "nextAttemptAt");
        correlationKey = normalizeOptional(correlationKey);
        providerReference = normalizeOptional(providerReference);
        lastError = normalizeOptional(lastError);
        if (attemptCount < 0) {
            throw new IllegalArgumentException("attemptCount deve ser >= 0");
        }
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts deve ser >= 1");
        }
        channelChain = normalizeChannelChain(channelChain);
        if (currentChannelIndex < 0 || currentChannelIndex >= channelChain.size()) {
            throw new IllegalArgumentException("currentChannelIndex inválido");
        }
        CanalComunicacaoInstitucional canalAtual = channelChain.get(currentChannelIndex);
        justificativas = normalizeJustificativas(justificativas);
        terminalAt = status.isTerminal() ? (terminalAt == null ? updatedAt : terminalAt) : terminalAt;
        hashIntegridade = hashIntegridade == null || hashIntegridade.isBlank()
                ? Hashes.sha256Hex(jobId + "|" + expedicaoUuid + "|" + canalAtual.name() + "|" + status.name() + "|" + attemptCount + "|" + updatedAt)
                : hashIntegridade.trim();
    }

    private static List<CanalComunicacaoInstitucional> normalizeChannelChain(List<CanalComunicacaoInstitucional> channelChain) {
        Set<CanalComunicacaoInstitucional> distinct = new LinkedHashSet<>();
        for (CanalComunicacaoInstitucional channel : channelChain == null ? List.<CanalComunicacaoInstitucional>of() : channelChain) {
            distinct.add(Objects.requireNonNull(channel, "channelChain contém canal nulo"));
        }
        if (distinct.isEmpty()) {
            throw new IllegalArgumentException("channelChain deve conter ao menos um canal");
        }
        return List.copyOf(distinct);
    }

    private static List<String> normalizeJustificativas(List<String> justificativas) {
        List<String> normalized = new ArrayList<>();
        for (String justificativa : justificativas == null ? List.<String>of() : justificativas) {
            if (justificativa != null && !justificativa.isBlank()) {
                normalized.add(justificativa);
            }
        }
        return List.copyOf(normalized);
    }


    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public CanalComunicacaoInstitucional currentChannel() {
        return channelChain.get(currentChannelIndex);
    }

    public boolean hasNextFallbackChannel() {
        return currentChannelIndex + 1 < channelChain.size();
    }

    public boolean canDispatch(Instant now) {
        return status.isDespachavel() && !nextAttemptAt.isAfter(now);
    }

    public InstitutionalDeliveryJob withProcessing(Instant now, String detail) {
        return mutate(StatusEntregaInstitucional.EM_PROCESSAMENTO, attemptCount, currentChannelIndex, now, now, null, providerReference, null, null, detail);
    }

    public InstitutionalDeliveryJob withEncaminhada(Instant now, String providerReference, String detail) {
        return mutate(StatusEntregaInstitucional.ENCAMINHADA, attemptCount + 1, currentChannelIndex, now, lastAttemptAtOr(now), now, providerReference, null, null, detail);
    }

    public InstitutionalDeliveryJob withEntregue(Instant now, String providerReference, String detail) {
        return mutate(StatusEntregaInstitucional.ENTREGUE, attemptCount + 1, currentChannelIndex, now, lastAttemptAtOr(now), now, providerReference, null, null, detail);
    }

    public InstitutionalDeliveryJob withRetry(Instant now, Instant nextAttemptAt, MotivoFalhaEntregaInstitucional reason, String detail) {
        return mutate(StatusEntregaInstitucional.AGUARDANDO_RETRY, attemptCount + 1, currentChannelIndex, now, now, null, null, reason, detail, detail);
    }

    public InstitutionalDeliveryJob withAdvancedFallback(Instant now, Instant nextAttemptAt, MotivoFalhaEntregaInstitucional reason, String detail) {
        if (!hasNextFallbackChannel()) {
            throw new IllegalStateException("Não existe fallback adicional disponível");
        }
        return mutate(StatusEntregaInstitucional.AGUARDANDO_RETRY, attemptCount + 1, currentChannelIndex + 1, nextAttemptAt, now, null, null, reason, detail, "fallback->" + channelChain.get(currentChannelIndex + 1).name());
    }

    public InstitutionalDeliveryJob withDeadLetter(Instant now, MotivoFalhaEntregaInstitucional reason, String detail) {
        return mutate(StatusEntregaInstitucional.MOVIDA_DLQ, attemptCount + 1, currentChannelIndex, now, now, now, null, reason, detail, detail);
    }

    public InstitutionalDeliveryJob requeue(Instant now, String detail) {
        return mutate(StatusEntregaInstitucional.AGUARDANDO_RETRY, attemptCount, currentChannelIndex, now, now, null, null, null, null, detail);
    }

    private Instant lastAttemptAtOr(Instant fallback) {
        return lastAttemptAt == null ? fallback : lastAttemptAt;
    }

    private InstitutionalDeliveryJob mutate(StatusEntregaInstitucional newStatus,
                                            int newAttemptCount,
                                            int newChannelIndex,
                                            Instant nextAt,
                                            Instant lastAt,
                                            Instant terminalAt,
                                            String newProviderReference,
                                            MotivoFalhaEntregaInstitucional newReason,
                                            String newLastError,
                                            String justification) {
        Instant now = lastAt == null ? Instant.now() : lastAt;
        List<String> merged = new ArrayList<>(justificativas);
        if (justification != null && !justification.isBlank()) {
            merged.add(justification);
        }
        return new InstitutionalDeliveryJob(
                jobId,
                expedicaoUuid,
                processoId,
                processoNumero,
                unidadeCodigo,
                caixaCodigo,
                destinatarioKind,
                papelProcessual,
                channelChain,
                newChannelIndex,
                newStatus,
                newAttemptCount,
                maxAttempts,
                createdAt,
                now,
                Objects.requireNonNull(nextAt, "nextAt"),
                lastAt,
                terminalAt,
                correlationKey,
                newProviderReference,
                newReason,
                newLastError,
                merged,
                Hashes.sha256Hex(jobId + "|" + expedicaoUuid + "|" + channelChain.get(newChannelIndex).name() + "|" + newStatus.name() + "|" + newAttemptCount + "|" + now)
        );
    }
}
