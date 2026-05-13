package com.tcc.pjb.backend.integration.datajud.feed;

import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;
import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudFeedBatchCommand;
import com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudTribunalRunCommand;
import com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudProcessoSnapshot;
import com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudFeedErrorSnapshot;
import com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudCheckpointUpdate;
import com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudCheckpointCommand;
import com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudFeedBulkResult;
import com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudFeedCheckpointSnapshot;
import com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudFeedRunSummary;
import com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudTribunalConfig;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.judicial.DataJudFeedCheckpoint;
import com.tcc.pjb.backend.model.repository.DataJudFeedCheckpointRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudCheckpointView;
import com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudProgressView;
import com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudEntryProjection;

@Service
public class DataJudFeedService {

    private final ProcessoRepository processoRepository;
    private final DataJudFeedCheckpointRepository checkpointRepository;
    private final DataJudFeedHttpClient httpClient;
    private final DataJudFeedPayloadAssembler assembler;
    private final AuditLedgerService auditLedger;
    private final ReadAfterWriteConsistencyPolicy readAfterWriteConsistencyPolicy;
    private final DataJudFeedProperties properties;

    public DataJudFeedService(ProcessoRepository processoRepository,
                              DataJudFeedCheckpointRepository checkpointRepository,
                              DataJudFeedHttpClient httpClient,
                              DataJudFeedPayloadAssembler assembler,
                              AuditLedgerService auditLedger,
                              ReadAfterWriteConsistencyPolicy readAfterWriteConsistencyPolicy,
                              DataJudFeedProperties properties) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.checkpointRepository = Objects.requireNonNull(checkpointRepository);
        this.httpClient = Objects.requireNonNull(httpClient);
        this.assembler = Objects.requireNonNull(assembler);
        this.auditLedger = Objects.requireNonNull(auditLedger);
        this.readAfterWriteConsistencyPolicy = Objects.requireNonNull(readAfterWriteConsistencyPolicy);
        this.properties = Objects.requireNonNull(properties);
    }

    @Transactional
    @PjbTransactionalBudget(operation = "datajud.feed.bulk-run.persist", maxMillis = 2500, critical = true)
    public DataJudFeedBulkResult runIncremental(DataJudTribunalRunCommand command) {
        Objects.requireNonNull(command);
        DataJudFeedRunSummary summary = runIncremental(command.tribunalCodigo());
        DataJudFeedCheckpoint checkpoint = checkpointRepository.findByTribunalCodigo(command.tribunalCodigo())
                .orElseGet(() -> DataJudFeedCheckpoint.init(command.tribunalCodigo()));
        return new DataJudFeedBulkResult(summary.tribunalCodigo(), summary.totalSent(), 1, new DataJudFeedCheckpointSnapshot(checkpoint.getTribunalCodigo(), checkpoint.getLastProcessoId() == null ? 0L : checkpoint.getLastProcessoId(), checkpoint.getTotalSent() == null ? 0L : checkpoint.getTotalSent(), checkpoint.getLastSentAt(), checkpoint.getLastError()), summary.completed());
    }

    @Transactional
    @PjbTransactionalBudget(operation = "datajud.feed.bulk-run.persist", maxMillis = 2500, critical = true)
    public DataJudFeedBulkResult runIncremental(DataJudFeedBatchCommand command) {
        Objects.requireNonNull(command);
        DataJudFeedRunSummary summary = runIncremental(command.tribunalCodigo());
        DataJudFeedCheckpoint checkpoint = checkpointRepository.findByTribunalCodigo(command.tribunalCodigo())
                .orElseGet(() -> DataJudFeedCheckpoint.init(command.tribunalCodigo()));
        return new DataJudFeedBulkResult(summary.tribunalCodigo(), summary.totalSent(), 1, new DataJudFeedCheckpointSnapshot(checkpoint.getTribunalCodigo(), checkpoint.getLastProcessoId() == null ? 0L : checkpoint.getLastProcessoId(), checkpoint.getTotalSent() == null ? 0L : checkpoint.getTotalSent(), checkpoint.getLastSentAt(), checkpoint.getLastError()), summary.completed());
    }


    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "datajud.feed.checkpoint-snapshot.read", maxMillis = 1200, critical = false)
    public DataJudFeedCheckpointSnapshot checkpointSnapshot(DataJudCheckpointCommand command) {
        Objects.requireNonNull(command);
        DataJudFeedCheckpoint checkpoint = checkpointRepository.findByTribunalCodigo(command.tribunalCodigo())
                .orElseGet(() -> DataJudFeedCheckpoint.init(command.tribunalCodigo()));
        return new DataJudFeedCheckpointSnapshot(checkpoint.getTribunalCodigo(), checkpoint.getLastProcessoId() == null ? 0L : checkpoint.getLastProcessoId(), checkpoint.getTotalSent() == null ? 0L : checkpoint.getTotalSent(), checkpoint.getLastSentAt(), checkpoint.getLastError());
    }

    public DataJudCheckpointUpdate checkpointUpdate(String tribunalCodigo) {
        DataJudFeedCheckpoint checkpoint = checkpointRepository.findByTribunalCodigo(tribunalCodigo)
                .orElseGet(() -> DataJudFeedCheckpoint.init(tribunalCodigo));
        return new DataJudCheckpointUpdate(tribunalCodigo, checkpoint.getLastProcessoId() == null ? 0L : checkpoint.getLastProcessoId(), checkpoint.getTotalSent() == null ? 0 : checkpoint.getTotalSent().intValue());
    }

    public DataJudFeedErrorSnapshot errorSnapshot(String tribunalCodigo) {
        DataJudFeedCheckpoint checkpoint = checkpointRepository.findByTribunalCodigo(tribunalCodigo)
                .orElseGet(() -> DataJudFeedCheckpoint.init(tribunalCodigo));
        return new DataJudFeedErrorSnapshot(tribunalCodigo, checkpoint.getLastError());
    }

    public DataJudProcessoSnapshot processoSnapshot(Processo processo) {
        return new DataJudProcessoSnapshot(processo.getId(), processo.getNumeroUnificado(), processo.getTribunal());
    }

    public DataJudTribunalConfig tribunalConfig(String tribunalCodigo) {
        return new DataJudTribunalConfig(tribunalCodigo, properties.enabled());
    }

    @Transactional
    @PjbTransactionalBudget(operation = "datajud.feed.incremental.persist", maxMillis = 4000, critical = true)
    @CircuitBreaker(name = "datajud-feed")
    @Retry(name = "datajud-feed")
    @Bulkhead(name = "datajud-feed")
    public DataJudFeedRunSummary runIncremental(String tribunalCodigo) {
        if (!properties.enabled()) {
            return new DataJudFeedRunSummary(tribunalCodigo, 0, 0L, false);
        }
        DataJudFeedCheckpoint checkpoint = checkpointRepository.findByTribunalCodigo(tribunalCodigo)
                .orElseGet(() -> DataJudFeedCheckpoint.init(tribunalCodigo));
        int totalSent = 0;
        int pages = 0;
        int maxPages = Math.max(1, properties.maxBatchesPerRun());
        long cursor = checkpoint.getLastProcessoId() == null ? 0L : checkpoint.getLastProcessoId();
        while (pages < maxPages) {
            Slice<Processo> slice = processoRepository.findDataJudFeedBatch(cursor,
                    tribunalCodigo,
                    List.of(StatusProcesso.ARQUIVADO),
                    PageRequest.of(0, Math.max(1, properties.batchSize())));
            if (!slice.hasContent()) {
                break;
            }
            List<DataJudFeedEntry> entries = slice.getContent().stream()
                    .map(assembler::toEntry)
                    .toList();
            try {
                httpClient.bulkIndex(entries);
                long lastId = entries.getLast().processoId();
                cursor = lastId;
                totalSent += entries.size();
                checkpoint.setLastProcessoId(lastId);
                checkpoint.setTotalSent(checkpoint.getTotalSent() + entries.size());
                checkpoint.setLastSentAt(Instant.now());
                checkpoint.setLastError(null);
                checkpoint.setUpdatedAt(Instant.now());
                checkpointRepository.save(checkpoint);
                readAfterWriteConsistencyPolicy.markWrite();
                auditLedger.appendSafely("DATAJUD_FEED_SENT", "DATAJUD", tribunalCodigo, "batch=" + entries.size() + " lastId=" + lastId);
            } catch (Exception e) {
                checkpoint.setLastError(e.getMessage());
                checkpoint.setUpdatedAt(Instant.now());
                checkpointRepository.save(checkpoint);
                readAfterWriteConsistencyPolicy.markWrite();
                auditLedger.appendSafely("DATAJUD_FEED_ERROR", "DATAJUD", tribunalCodigo, "erro=" + e.getMessage());
                break;
            }
            pages++;
            if (entries.size() < Math.max(1, properties.batchSize())) {
                break;
            }
        }
        long lastProcessoId = checkpoint.getLastProcessoId() == null ? 0L : checkpoint.getLastProcessoId();
        return new DataJudFeedRunSummary(tribunalCodigo, totalSent, lastProcessoId, true);
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "datajud.feed.checkpoint.read", maxMillis = 1200, critical = false)
    public DataJudCheckpointView checkpointView(String tribunalCodigo) {
        DataJudFeedCheckpoint checkpoint = checkpointRepository.findByTribunalCodigo(tribunalCodigo)
                .orElseGet(() -> DataJudFeedCheckpoint.init(tribunalCodigo));
        return new DataJudCheckpointView(tribunalCodigo, checkpoint.getLastProcessoId(), checkpoint.getTotalSent(), checkpoint.getLastSentAt(), checkpoint.getLastError());
    }

    public DataJudProgressView progress(String tribunalCodigo, int batchSent) {
        DataJudFeedCheckpoint checkpoint = checkpointRepository.findByTribunalCodigo(tribunalCodigo)
                .orElseGet(() -> DataJudFeedCheckpoint.init(tribunalCodigo));
        return new DataJudProgressView(tribunalCodigo, batchSent, checkpoint.getTotalSent(), checkpoint.getLastError() == null);
    }

    public DataJudEntryProjection projection(Processo processo) {
        return new DataJudEntryProjection(processo.getId(), processo.getNumeroUnificado(), processo.getTribunal(), processo.getClasseTpuCodigo());
    }


    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "datajud.feed.checkpoint-audit.read", maxMillis = 1200, critical = false)
    public com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudCheckpointAuditSnapshot checkpointAudit(String tribunalCodigo) {
        DataJudFeedCheckpoint checkpoint = checkpointRepository.findByTribunalCodigo(tribunalCodigo)
                .orElseGet(() -> DataJudFeedCheckpoint.init(tribunalCodigo));
        return new com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudCheckpointAuditSnapshot(
                tribunalCodigo,
                checkpoint.getLastProcessoId(),
                checkpoint.getTotalSent(),
                checkpoint.getLastSentAt()
        );
    }

    public com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudTribunalProgressSnapshot tribunalProgress(String tribunalCodigo, int batchSent) {
        DataJudProgressView progress = progress(tribunalCodigo, batchSent);
        return new com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudTribunalProgressSnapshot(
                progress.tribunalCodigo(),
                progress.batchSent(),
                progress.totalSent(),
                progress.ok()
        );
    }

    public com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudFeedBatchSnapshot batchSnapshot(com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudFeedRunSummary summary) {
        return new com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudFeedBatchSnapshot(
                summary.tribunalCodigo(),
                Math.max(1, properties.batchSize()),
                summary.lastProcessoId(),
                properties.enabled()
        );
    }

    public com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudFeedWindowResult consultarJanela(com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudFeedWindowCommand command) {
        Objects.requireNonNull(command);
        return new com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudFeedWindowResult(
                command.tribunalCodigo(),
                command.fromProcessoId(),
                command.batchSize(),
                properties.enabled()
        );
    }



    public com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudCheckpointQueryResult consultarCheckpoint(com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudCheckpointQuery query) {
        Objects.requireNonNull(query);
        return new com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudCheckpointQueryResult(
                checkpointView(query.tribunalCodigo()),
                checkpointAudit(query.tribunalCodigo())
        );
    }

    public com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudFeedHealthSnapshot healthSnapshot(String tribunalCodigo) {
        var checkpoint = checkpointView(tribunalCodigo);
        return new com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudFeedHealthSnapshot(
                tribunalCodigo,
                properties.enabled(),
                checkpoint.totalSent(),
                semErroMaterial(checkpoint.lastError())
        );
    }

    public com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudTribunalWindowSnapshot tribunalWindow(String tribunalCodigo) {
        return new com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudTribunalWindowSnapshot(
                tribunalCodigo,
                checkpointView(tribunalCodigo).lastProcessoId(),
                Math.max(1, properties.batchSize())
        );
    }

    public com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudEntryAuditSnapshot entryAudit(com.tcc.pjb.backend.model.entity.Processo processo) {
        return new com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudEntryAuditSnapshot(
                processo.getId(),
                processo.getTribunal(),
                processo.getClasseTpuCodigo(),
                processo.getStatusProcesso() == null ? null : processo.getStatusProcesso().name()
        );
    }



    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "datajud.feed.window.read", maxMillis = 1200, critical = false)
    public com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudWindowView windowView(com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudWindowQuery query) {
        Objects.requireNonNull(query);
        var checkpoint = checkpointView(query.tribunalCodigo());
        return new com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudWindowView(query.tribunalCodigo(), checkpoint.lastProcessoId(), Math.max(1, properties.batchSize()), properties.enabled());
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "datajud.feed.tribunal-health.read", maxMillis = 1200, critical = false)
    public com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudTribunalHealthResult tribunalHealth(com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudTribunalHealthQuery query) {
        Objects.requireNonNull(query);
        return new com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudTribunalHealthResult(healthSnapshot(query.tribunalCodigo()), tribunalWindow(query.tribunalCodigo()));
    }

    public com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudEntryView entryView(Processo processo) {
        return new com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudEntryView(processo.getId(), processo.getNumeroUnificado(), processo.getTribunal(), processo.getClasseTpuCodigo());
    }



@Transactional(readOnly = true)
@PjbTransactionalBudget(operation = "datajud.feed.checkpoint-view.read", maxMillis = 1200, critical = false)
public com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudCheckpointViewResult checkpointViewResult(com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudCheckpointViewQuery query) {
    java.util.Objects.requireNonNull(query);
    return new com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudCheckpointViewResult(checkpointView(query.tribunalCodigo()), checkpointAudit(query.tribunalCodigo()));
}

@Transactional(readOnly = true)
@PjbTransactionalBudget(operation = "datajud.feed.execution-health.read", maxMillis = 1200, critical = false)
public com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudFeedExecutionHealth executionHealth(String tribunalCodigo) {
    var checkpoint = checkpointView(tribunalCodigo);
    return new com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudFeedExecutionHealth(tribunalCodigo, properties.enabled(), semErroMaterial(checkpoint.lastError()), checkpoint.totalSent());
}

@Transactional(readOnly = true)
@PjbTransactionalBudget(operation = "datajud.feed.audit-window.read", maxMillis = 1200, critical = false)
public com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudFeedAuditWindow auditWindow(String tribunalCodigo) {
    var checkpoint = checkpointView(tribunalCodigo);
    return new com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudFeedAuditWindow(tribunalCodigo, checkpoint.lastProcessoId(), Math.max(1, properties.batchSize()), properties.enabled());
}

public com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudTribunalAuditEntry tribunalAuditEntry(String tribunalCodigo) {
    var checkpoint = checkpointView(tribunalCodigo);
    return new com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudTribunalAuditEntry(tribunalCodigo, checkpoint.totalSent(), checkpoint.lastSentAt());
}


    public com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudHealthResult health(com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudHealthQuery query) {
        java.util.Objects.requireNonNull(query);
        return new com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudHealthResult(healthSnapshot(query.tribunalCodigo()), checkpointView(query.tribunalCodigo()));
    }

    public com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudCheckpointHealthView checkpointHealthView(String tribunalCodigo) {
        var checkpoint = checkpointView(tribunalCodigo);
        boolean healthy = semErroMaterial(checkpoint.lastError());
        return new com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudCheckpointHealthView(tribunalCodigo, checkpoint.lastProcessoId(), checkpoint.totalSent(), healthy);
    }

    public com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudWindowHealthSnapshot windowHealth(String tribunalCodigo) {
        var checkpoint = checkpointView(tribunalCodigo);
        boolean healthy = semErroMaterial(checkpoint.lastError());
        return new com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudWindowHealthSnapshot(tribunalCodigo, checkpoint.lastProcessoId(), Math.max(1, properties.batchSize()), properties.enabled(), healthy);
    }

    public com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudTribunalExecutionView executionView(String tribunalCodigo) {
        var checkpoint = checkpointView(tribunalCodigo);
        boolean healthy = semErroMaterial(checkpoint.lastError());
        return new com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudTribunalExecutionView(tribunalCodigo, checkpoint.totalSent(), checkpoint.lastProcessoId(), healthy);
    }

    public com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudTribunalAuditView tribunalAuditView(String tribunalCodigo) {
        var checkpointAudit = checkpointAudit(tribunalCodigo);
        var progress = tribunalProgress(tribunalCodigo, 0);
        return new com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudTribunalAuditView(tribunalCodigo, checkpointAudit, progress);
    }

    private boolean semErroMaterial(String lastError) {
        return lastError == null || lastError.isBlank() || "none".equalsIgnoreCase(lastError.trim()) || "sem_erro".equalsIgnoreCase(lastError.trim());
    }

}
