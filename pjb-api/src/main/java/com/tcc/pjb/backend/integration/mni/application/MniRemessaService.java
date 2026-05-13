package com.tcc.pjb.backend.integration.mni.application;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.integration.mni.adapter.MniProcessoPayloadAssembler;
import com.tcc.pjb.backend.integration.mni.domain.MniHttpResponse;
import com.tcc.pjb.backend.integration.mni.domain.MniRemessaBatchCommand;
import com.tcc.pjb.backend.integration.mni.domain.MniRemessaCommand;
import com.tcc.pjb.backend.integration.mni.domain.MniRemessaRequest;
import com.tcc.pjb.backend.integration.mni.domain.MniRemessaResult;
import com.tcc.pjb.backend.integration.mni.domain.MniReprocessamentoSummary;
import com.tcc.pjb.backend.integration.mni.domain.MniStatusRemessa;
import com.tcc.pjb.backend.integration.mni.infra.MniHttpClient;
import com.tcc.pjb.backend.integration.mni.infra.MniRemessaProperties;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.judicial.MniRemessa;
import com.tcc.pjb.backend.model.repository.MniRemessaRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.integration.mni.domain.MniConsultaRemessaCommand;
import com.tcc.pjb.backend.integration.mni.domain.MniConsultaRemessaResult;
import com.tcc.pjb.backend.integration.mni.domain.MniRemessaAuditSnapshot;
import com.tcc.pjb.backend.integration.mni.domain.MniRemessaProjection;

@Service
public class MniRemessaService {

    private final ProcessoRepository processoRepository;
    private final MniRemessaRepository remessaRepository;
    private final MniProcessoPayloadAssembler payloadAssembler;
    private final MniHttpClient mniHttpClient;
    private final AuditLedgerService auditLedger;
    private final ReadAfterWriteConsistencyPolicy readAfterWriteConsistencyPolicy;
    private final MniRemessaProperties properties;

    public MniRemessaService(ProcessoRepository processoRepository,
                             MniRemessaRepository remessaRepository,
                             MniProcessoPayloadAssembler payloadAssembler,
                             MniHttpClient mniHttpClient,
                             AuditLedgerService auditLedger,
                             ReadAfterWriteConsistencyPolicy readAfterWriteConsistencyPolicy,
                             MniRemessaProperties properties) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.remessaRepository = Objects.requireNonNull(remessaRepository);
        this.payloadAssembler = Objects.requireNonNull(payloadAssembler);
        this.mniHttpClient = Objects.requireNonNull(mniHttpClient);
        this.auditLedger = Objects.requireNonNull(auditLedger);
        this.readAfterWriteConsistencyPolicy = Objects.requireNonNull(readAfterWriteConsistencyPolicy);
        this.properties = Objects.requireNonNull(properties);
    }

    @Transactional
    @CircuitBreaker(name = "mni-remessa")
    @Retry(name = "mni-remessa")
    @Bulkhead(name = "mni-remessa")
    public MniRemessaResult enviar(MniRemessaCommand command) {
        Objects.requireNonNull(command);
        return enviar(new MniRemessaRequest(command.processoId(), command.tribunalDestino(), command.motivo()));
    }

    @Transactional
    @CircuitBreaker(name = "mni-remessa")
    @Retry(name = "mni-remessa")
    @Bulkhead(name = "mni-remessa")
    public MniRemessaResult enviar(MniRemessaRequest req) {
        Processo processo = processoRepository.findById(req.processoId())
                .orElseThrow(() -> new IllegalArgumentException("Processo não encontrado: " + req.processoId()));
        return remessaRepository.findByProcessoIdAndTribunalDestinoAndMotivo(req.processoId(), req.tribunalDestino(), req.motivo())
                .filter(existing -> existing.getStatus() == MniStatusRemessa.CONFIRMED)
                .map(existing -> MniRemessaResult.alreadyConfirmed(existing.getProtocoloDestino()))
                .orElseGet(() -> doEnviar(processo, req));
    }

    @Transactional
    public MniReprocessamentoSummary reprocessarPendentes(MniRemessaBatchCommand command) {
        Objects.requireNonNull(command);
        int processadas = 0;
        int superseded = 0;
        int ignoradas = 0;
        if (!properties.enabled()) {
            return new MniReprocessamentoSummary(0, 0, 0);
        }
        List<MniRemessa> candidatas = remessaRepository.findRetryCandidates(Instant.now());
        int limite = Math.max(1, command.limit());
        for (MniRemessa remessa : candidatas) {
            if (processadas >= limite) {
                break;
            }
            Processo processo = processoRepository.findById(remessa.getProcessoId()).orElse(null);
            if (processo == null) {
                remessa.setStatus(MniStatusRemessa.SUPERSEDED);
                remessa.setFailureReason("Processo origem não encontrado para retry");
                remessaRepository.save(remessa);
                readAfterWriteConsistencyPolicy.markWrite();
                superseded++;
                continue;
            }
            MniRemessaResult resultado = doEnviar(processo, new MniRemessaRequest(remessa.getProcessoId(), remessa.getTribunalDestino(), remessa.getMotivo()));
            if (resultado.success()) {
                processadas++;
            } else {
                ignoradas++;
            }
        }
        return new MniReprocessamentoSummary(processadas, superseded, ignoradas);
    }

    @Transactional
    public int reprocessarPendentes() {
        if (!properties.enabled()) {
            return 0;
        }
        List<MniRemessa> candidatas = remessaRepository.findRetryCandidates(Instant.now());
        int processadas = 0;
        int limite = Math.max(1, properties.reconciliationBatchSize());
        for (MniRemessa remessa : candidatas) {
            if (processadas >= limite) {
                break;
            }
            Processo processo = processoRepository.findById(remessa.getProcessoId()).orElse(null);
            if (processo == null) {
                remessa.setStatus(MniStatusRemessa.SUPERSEDED);
                remessa.setFailureReason("Processo origem não encontrado para retry");
                remessaRepository.save(remessa);
                readAfterWriteConsistencyPolicy.markWrite();
                continue;
            }
            doEnviar(processo, new MniRemessaRequest(remessa.getProcessoId(), remessa.getTribunalDestino(), remessa.getMotivo()));
            processadas++;
        }
        return processadas;
    }

    protected MniRemessaResult doEnviar(Processo processo, MniRemessaRequest req) {
        MniRemessa entity = remessaRepository.findByProcessoIdAndTribunalDestinoAndMotivo(req.processoId(), req.tribunalDestino(), req.motivo())
                .orElseGet(() -> MniRemessa.builder()
                        .processoId(req.processoId())
                        .tribunalDestino(req.tribunalDestino())
                        .motivo(req.motivo())
                        .status(MniStatusRemessa.PENDING)
                        .tentativas(0)
                        .maxTentativas(Math.max(1, properties.retryMaxTentativas()))
                        .createdAt(Instant.now())
                        .build());
        try {
            String xml = payloadAssembler.toXml(processo);
            entity.setMniPayloadHash(Hashes.sha256Hex(xml));
            entity.setStatus(MniStatusRemessa.PENDING);
            MniHttpResponse response = mniHttpClient.enviarAutos(req.tribunalDestino(), xml);
            entity.setProtocoloDestino(response.protocolo());
            entity.setStatus(MniStatusRemessa.CONFIRMED);
            entity.setSentAt(Instant.now());
            entity.setConfirmedAt(Instant.now());
            remessaRepository.save(entity);
            readAfterWriteConsistencyPolicy.markWrite();
            auditLedger.appendSafely("MNI_REMESSA_OK", "PROCESSO", String.valueOf(processo.getId()), "tribunal=" + req.tribunalDestino() + " protocolo=" + response.protocolo());
            return MniRemessaResult.success(response.protocolo());
        } catch (Exception e) {
            int attempts = entity.getTentativas() == null ? 0 : entity.getTentativas();
            entity.setTentativas(attempts + 1);
            entity.setStatus(entity.getTentativas() >= Math.max(1, entity.getMaxTentativas()) ? MniStatusRemessa.SUPERSEDED : MniStatusRemessa.FAILED);
            entity.setFailureReason(e.getMessage());
            entity.setProximoRetryEm(Instant.now().plus(30L * Math.max(1, entity.getTentativas()), ChronoUnit.MINUTES));
            remessaRepository.save(entity);
            readAfterWriteConsistencyPolicy.markWrite();
            auditLedger.appendSafely("MNI_REMESSA_FAIL", "PROCESSO", String.valueOf(processo.getId()), "tribunal=" + req.tribunalDestino() + " erro=" + e.getMessage());
            return MniRemessaResult.failed(e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public MniConsultaRemessaResult consultar(MniConsultaRemessaCommand command) {
        Objects.requireNonNull(command);
        MniRemessa remessa = remessaRepository.findById(command.remessaId())
                .orElseThrow(() -> new IllegalArgumentException("Remessa MNI não encontrada: " + command.remessaId()));
        MniRemessaProjection projection = new MniRemessaProjection(remessa.getId(), remessa.getProcessoId(), remessa.getTribunalDestino(), remessa.getMotivo(), remessa.getStatus().name(), remessa.getCreatedAt());
        MniRemessaAuditSnapshot audit = new MniRemessaAuditSnapshot(remessa.getId(), remessa.getMniPayloadHash(), remessa.getFailureReason());
        return new MniConsultaRemessaResult(projection, audit);
    }



    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.integration.mni.domain.MniRemessaStatusSnapshot statusSnapshot(Long remessaId) {
        var remessa = remessaRepository.findById(remessaId)
                .orElseThrow(() -> new IllegalArgumentException("Remessa MNI não encontrada: " + remessaId));
        return new com.tcc.pjb.backend.integration.mni.domain.MniRemessaStatusSnapshot(
                remessa.getId(),
                remessa.getStatus().name(),
                remessa.getProtocoloDestino(),
                remessa.getSentAt(),
                remessa.getConfirmedAt()
        );
    }



    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.integration.mni.domain.MniRemessaTimelineResult timeline(com.tcc.pjb.backend.integration.mni.domain.MniRemessaTimelineQuery query) {
        Objects.requireNonNull(query);
        MniRemessa remessa = remessaRepository.findById(query.remessaId())
                .orElseThrow(() -> new IllegalArgumentException("Remessa MNI não encontrada: " + query.remessaId()));
        java.util.ArrayList<com.tcc.pjb.backend.integration.mni.domain.MniRemessaTimelineEntry> entries = new java.util.ArrayList<>();
        entries.add(new com.tcc.pjb.backend.integration.mni.domain.MniRemessaTimelineEntry("CRIADA", remessa.getCreatedAt(), remessa.getTribunalDestino()));
        if (remessa.getSentAt() != null) entries.add(new com.tcc.pjb.backend.integration.mni.domain.MniRemessaTimelineEntry("ENVIADA", remessa.getSentAt(), remessa.getProtocoloDestino()));
        if (remessa.getConfirmedAt() != null) entries.add(new com.tcc.pjb.backend.integration.mni.domain.MniRemessaTimelineEntry("CONFIRMADA", remessa.getConfirmedAt(), remessa.getStatus().name()));
        if (remessa.getFailureReason() != null && !remessa.getFailureReason().isBlank()) entries.add(new com.tcc.pjb.backend.integration.mni.domain.MniRemessaTimelineEntry("FALHA", remessa.getProximoRetryEm(), remessa.getFailureReason()));
        return new com.tcc.pjb.backend.integration.mni.domain.MniRemessaTimelineResult(query.remessaId(), java.util.List.copyOf(entries));
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.integration.mni.domain.MniPayloadView payloadView(Long remessaId) {
        MniRemessa remessa = remessaRepository.findById(remessaId)
                .orElseThrow(() -> new IllegalArgumentException("Remessa MNI não encontrada: " + remessaId));
        return new com.tcc.pjb.backend.integration.mni.domain.MniPayloadView(remessa.getMniPayloadHash(), remessa.getMotivo(), remessa.getStatus().name());
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.integration.mni.domain.MniRemessaHealthSnapshot healthSnapshot(Long remessaId) {
        MniRemessa remessa = remessaRepository.findById(remessaId)
                .orElseThrow(() -> new IllegalArgumentException("Remessa MNI não encontrada: " + remessaId));
        return new com.tcc.pjb.backend.integration.mni.domain.MniRemessaHealthSnapshot(remessa.getId(), remessa.getStatus().name(), remessa.getTentativas() == null ? 0 : remessa.getTentativas(), remessa.getFailureReason() != null && !remessa.getFailureReason().isBlank());
    }



@Transactional(readOnly = true)
public com.tcc.pjb.backend.integration.mni.domain.MniRemessaHealthResult health(com.tcc.pjb.backend.integration.mni.domain.MniRemessaHealthQuery query) {
    java.util.Objects.requireNonNull(query);
    var remessa = remessaRepository.findById(query.remessaId()).orElseThrow(() -> new IllegalArgumentException("Remessa MNI não encontrada: " + query.remessaId()));
    return new com.tcc.pjb.backend.integration.mni.domain.MniRemessaHealthResult(remessa.getId(), remessa.getStatus().name(), remessa.getConfirmedAt() != null, remessa.getMniPayloadHash() != null && !remessa.getMniPayloadHash().isBlank());
}

@Transactional(readOnly = true)
public com.tcc.pjb.backend.integration.mni.domain.MniTribunalEndpointView endpointView(String tribunalCodigo) {
    return new com.tcc.pjb.backend.integration.mni.domain.MniTribunalEndpointView(tribunalCodigo, null, null);
}

@Transactional(readOnly = true)
public com.tcc.pjb.backend.integration.mni.domain.MniRemessaWindowSnapshot windowSnapshot(Long remessaId) {
    var remessa = remessaRepository.findById(remessaId).orElseThrow(() -> new IllegalArgumentException("Remessa MNI não encontrada: " + remessaId));
    return new com.tcc.pjb.backend.integration.mni.domain.MniRemessaWindowSnapshot(remessa.getId(), remessa.getProximoRetryEm(), remessa.getTentativas() == null ? 0 : remessa.getTentativas(), remessa.getMaxTentativas() == null ? 0 : remessa.getMaxTentativas());
}
}
