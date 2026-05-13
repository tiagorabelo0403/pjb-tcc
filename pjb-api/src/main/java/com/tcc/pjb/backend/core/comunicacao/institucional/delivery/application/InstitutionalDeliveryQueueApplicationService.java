package com.tcc.pjb.backend.core.comunicacao.institucional.delivery.application;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.comunicacao.institucional.audit.application.InstitutionalCommunicationAuditApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.delivery.domain.InstitutionalDeadLetterEntry;
import com.tcc.pjb.backend.core.comunicacao.institucional.delivery.domain.InstitutionalDeliveryAttempt;
import com.tcc.pjb.backend.core.comunicacao.institucional.delivery.domain.InstitutionalDeliveryDispatchResult;
import com.tcc.pjb.backend.core.comunicacao.institucional.delivery.domain.InstitutionalDeliveryJob;
import com.tcc.pjb.backend.core.comunicacao.institucional.delivery.domain.InstitutionalDeliveryProcessingSummary;
import com.tcc.pjb.backend.core.comunicacao.institucional.delivery.infrastructure.InstitutionalDeliveryAttemptStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.delivery.infrastructure.InstitutionalDeliveryChannelDispatcher;
import com.tcc.pjb.backend.core.comunicacao.institucional.delivery.infrastructure.InstitutionalDeliveryDeadLetterStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.delivery.infrastructure.InstitutionalDeliveryJobStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.domain.InstitutionalInboxItem;
import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.infrastructure.InstitutionalInboxStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.model.CanalEntregaInstitucional;
import com.tcc.pjb.backend.core.comunicacao.institucional.routing.PlanoEntregaInstitucional;
import com.tcc.pjb.backend.core.comunicacao.institucional.routing.ResolucaoRoteamentoInstitucionalResult;
import com.tcc.pjb.backend.core.comunicacao.judicial.CitacaoIntimacaoEngine;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.CanalComunicacaoInstitucional;
import com.tcc.pjb.backend.model.entity.enums.MotivoFalhaEntregaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.StatusEntregaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.StatusTentativaEntregaInstitucional;

@Service
public class InstitutionalDeliveryQueueApplicationService {

    private final InstitutionalDeliveryJobStateRepository jobRepository;
    private final InstitutionalDeliveryAttemptStateRepository attemptRepository;
    private final InstitutionalDeliveryDeadLetterStateRepository deadLetterRepository;
    private final InstitutionalDeliveryChannelDispatcher channelDispatcher;
    private final InstitutionalInboxStateRepository inboxRepository;
    private final InstitutionalCommunicationAuditApplicationService auditService;

    public InstitutionalDeliveryQueueApplicationService(InstitutionalDeliveryJobStateRepository jobRepository,
                                                        InstitutionalDeliveryAttemptStateRepository attemptRepository,
                                                        InstitutionalDeliveryDeadLetterStateRepository deadLetterRepository,
                                                        InstitutionalDeliveryChannelDispatcher channelDispatcher,
                                                        InstitutionalInboxStateRepository inboxRepository,
                                                        InstitutionalCommunicationAuditApplicationService auditService) {
        this.jobRepository = Objects.requireNonNull(jobRepository);
        this.attemptRepository = Objects.requireNonNull(attemptRepository);
        this.deadLetterRepository = Objects.requireNonNull(deadLetterRepository);
        this.channelDispatcher = Objects.requireNonNull(channelDispatcher);
        this.inboxRepository = Objects.requireNonNull(inboxRepository);
        this.auditService = Objects.requireNonNull(auditService);
    }

    @Transactional
    public InstitutionalDeliveryJob enfileirar(Processo processo,
                                               CitacaoIntimacaoEngine.ExpedicaoResponse response,
                                               ResolucaoRoteamentoInstitucionalResult roteamento) {
        Objects.requireNonNull(processo, "processo");
        Objects.requireNonNull(response, "response");
        Objects.requireNonNull(roteamento, "roteamento");
        Instant now = response.expedidaEm() == null ? Instant.now() : response.expedidaEm();
        List<CanalComunicacaoInstitucional> chain = orderedChannels(roteamento.planoEntrega());
        List<String> justificativas = new ArrayList<>(roteamento.justificativas());
        justificativas.add("deliveryJobPrincipal=" + chain.getFirst().name());
        String jobId = UUID.nameUUIDFromBytes((response.expedicaoUuid() + "|DELIVERY").getBytes(StandardCharsets.UTF_8)).toString();
        InstitutionalDeliveryJob job = new InstitutionalDeliveryJob(
                jobId,
                response.expedicaoUuid(),
                processo.getId(),
                firstNonBlank(processo.getNumeroProcesso(), processo.getNumeroUnificado()),
                roteamento.alvo().unidade().codigo(),
                roteamento.alvo().caixa().codigo(),
                roteamento.alvo().destinatarioKind(),
                roteamento.alvo().papelProcessual(),
                chain,
                0,
                StatusEntregaInstitucional.PENDENTE,
                0,
                4,
                now,
                now,
                now,
                null,
                null,
                Hashes.sha256Hex(response.expedicaoUuid() + "|" + roteamento.alvo().unidade().codigo() + "|" + chain),
                null,
                null,
                null,
                justificativas,
                null
        );
        InstitutionalDeliveryJob persisted = jobRepository.save(job);
        loadInboxItem(response.expedicaoUuid()).ifPresent(item -> auditService.registrarEntregaEnfileirada(item, persisted));
        return persisted;
    }

    @Transactional
    public InstitutionalDeliveryProcessingSummary processarPendencias(int limit) {
        Instant now = Instant.now();
        int delivered = 0;
        int handedOff = 0;
        int retried = 0;
        int deadLettered = 0;
        List<InstitutionalDeliveryJob> selected = jobRepository.findDispatchable(limit, now);
        for (InstitutionalDeliveryJob current : selected) {
            InstitutionalDeliveryJob processing = jobRepository.save(current.withProcessing(now, "processamento_institucional"));
            InstitutionalDeliveryDispatchResult result = channelDispatcher.dispatch(processing);
            Instant endedAt = Instant.now();
            InstitutionalDeliveryAttempt attempt = new InstitutionalDeliveryAttempt(
                    UUID.nameUUIDFromBytes((processing.jobId() + "|" + (processing.attemptCount() + 1) + "|" + endedAt).getBytes(StandardCharsets.UTF_8)).toString(),
                    processing.jobId(),
                    processing.expedicaoUuid(),
                    processing.attemptCount() + 1,
                    processing.currentChannel(),
                    result.status(),
                    now,
                    endedAt,
                    result.providerReference(),
                    result.providerStatus(),
                    result.failureReason(),
                    result.transientFailure(),
                    result.detail(),
                    null
            );
            attemptRepository.save(attempt);
            Optional<InstitutionalInboxItem> item = loadInboxItem(processing.expedicaoUuid());
            item.ifPresent(inbox -> auditService.registrarTentativaEntrega(inbox, processing, attempt));
            if (result.status().isSucessoOperacional()) {
                InstitutionalDeliveryJob updated = result.status() == StatusTentativaEntregaInstitucional.ENTREGUE
                        ? processing.withEntregue(endedAt, result.providerReference(), result.detail())
                        : processing.withEncaminhada(endedAt, result.providerReference(), result.detail());
                jobRepository.save(updated);
                item.ifPresent(inbox -> {
                    if (updated.status() == StatusEntregaInstitucional.ENTREGUE) {
                        auditService.registrarEntregaConfirmada(inbox, updated, result.detail());
                    } else {
                        auditService.registrarEntregaEncaminhada(inbox, updated, result.detail());
                    }
                });
                if (updated.status() == StatusEntregaInstitucional.ENTREGUE) {
                    delivered++;
                } else {
                    handedOff++;
                }
                continue;
            }

            if (result.status() == StatusTentativaEntregaInstitucional.RETRY_AGENDADO && processing.attemptCount() + 1 < processing.maxAttempts()) {
                Instant next = endedAt.plus(retryBackoff(processing.attemptCount() + 1));
                InstitutionalDeliveryJob updated = processing.withRetry(endedAt, next, result.failureReason(), result.detail());
                jobRepository.save(updated);
                item.ifPresent(inbox -> auditService.registrarEntregaRetryAgendado(inbox, updated, result.detail()));
                retried++;
                continue;
            }

            if (processing.hasNextFallbackChannel()) {
                InstitutionalDeliveryJob updated = processing.withAdvancedFallback(
                        endedAt,
                        endedAt.plusSeconds(5),
                        result.failureReason() == null ? MotivoFalhaEntregaInstitucional.EXAURIDA_POLITICA_RETRY : result.failureReason(),
                        result.detail() == null ? "fallback_automatico" : result.detail()
                );
                jobRepository.save(updated);
                item.ifPresent(inbox -> auditService.registrarEntregaRetryAgendado(inbox, updated, "fallback para " + updated.currentChannel().name()));
                retried++;
                continue;
            }

            InstitutionalDeliveryJob dead = processing.withDeadLetter(
                    endedAt,
                    result.failureReason() == null ? MotivoFalhaEntregaInstitucional.EXAURIDA_POLITICA_RETRY : result.failureReason(),
                    result.detail() == null ? "falha terminal institucional" : result.detail()
            );
            jobRepository.save(dead);
            InstitutionalDeadLetterEntry dlq = new InstitutionalDeadLetterEntry(
                    UUID.nameUUIDFromBytes((dead.jobId() + "|DLQ|" + endedAt).getBytes(StandardCharsets.UTF_8)).toString(),
                    dead.jobId(),
                    dead.expedicaoUuid(),
                    dead.processoId(),
                    dead.processoNumero(),
                    dead.unidadeCodigo(),
                    dead.caixaCodigo(),
                    dead.currentChannel(),
                    dead.lastFailureReason(),
                    dead.attemptCount(),
                    dead.lastError(),
                    dead.justificativas(),
                    endedAt,
                    null
            );
            deadLetterRepository.save(dlq);
            item.ifPresent(inbox -> {
                auditService.registrarEntregaFalhaTerminal(inbox, dead, dead.lastError());
                auditService.registrarEntregaMovidaDlq(inbox, dead, dead.lastError());
            });
            deadLettered++;
        }
        return new InstitutionalDeliveryProcessingSummary(selected.size(), delivered, handedOff, retried, deadLettered);
    }

    @Transactional(readOnly = true)
    public Optional<InstitutionalDeliveryJob> consultarJob(String jobId) {
        return jobRepository.findByJobId(jobId);
    }

    @Transactional(readOnly = true)
    public List<InstitutionalDeliveryJob> listarEntregas(Long processoId, String expedicaoUuid) {
        if (processoId != null) {
            return jobRepository.findByProcessoId(processoId);
        }
        if (expedicaoUuid != null && !expedicaoUuid.isBlank()) {
            return jobRepository.findByExpedicaoUuid(expedicaoUuid);
        }
        return List.of();
    }

    @Transactional(readOnly = true)
    public List<InstitutionalDeadLetterEntry> listarDlq(Long processoId, String expedicaoUuid) {
        if (processoId != null) {
            return deadLetterRepository.findByProcessoId(processoId);
        }
        if (expedicaoUuid != null && !expedicaoUuid.isBlank()) {
            return deadLetterRepository.findByExpedicaoUuid(expedicaoUuid);
        }
        return List.of();
    }

    @Transactional
    public InstitutionalDeliveryJob reprocessar(String jobId, String detalhe) {
        InstitutionalDeliveryJob current = jobRepository.findByJobId(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job institucional não encontrado: " + jobId));
        InstitutionalDeliveryJob updated = current.requeue(Instant.now(), detalhe == null || detalhe.isBlank() ? "reprocessamento_manual" : detalhe);
        jobRepository.save(updated);
        loadInboxItem(updated.expedicaoUuid()).ifPresent(item -> auditService.registrarEntregaRetryAgendado(item, updated, detalhe));
        return updated;
    }

    private Optional<InstitutionalInboxItem> loadInboxItem(String expedicaoUuid) {
        return inboxRepository.findByExpedicaoUuid(expedicaoUuid);
    }

    private List<CanalComunicacaoInstitucional> orderedChannels(PlanoEntregaInstitucional plano) {
        Set<CanalComunicacaoInstitucional> ordered = new LinkedHashSet<>();
        ordered.add(plano.canalPrincipal().canal());
        plano.canaisFallback().stream().map(CanalEntregaInstitucional::canal).forEach(ordered::add);
        return List.copyOf(ordered);
    }

    private Duration retryBackoff(int attempt) {
        long seconds = switch (Math.max(1, attempt)) {
            case 1 -> 30;
            case 2 -> 120;
            case 3 -> 600;
            default -> 1800;
        };
        return Duration.ofSeconds(seconds);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
