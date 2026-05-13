package com.tcc.pjb.backend.integration.judicial;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

import com.tcc.pjb.backend.core.procedural.ProceduralConnectorExecutionReport;
import com.tcc.pjb.backend.core.procedural.ProceduralSubmissionBlueprintReport;
import com.tcc.pjb.backend.model.entity.Processo;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class JudicialConnectorLifecycleService {

    private final JudicialProtocolSubmissionService submissionService;
    private final ProcessSyncService processSyncService;

    public JudicialConnectorLifecycleService(JudicialProtocolSubmissionService submissionService,
                                            ProcessSyncService processSyncService) {
        this.submissionService = submissionService;
        this.processSyncService = processSyncService;
    }

    @CircuitBreaker(name = "judicial-connector-lifecycle")
    @Retry(name = "judicial-connector-lifecycle")
    @Bulkhead(name = "judicial-connector-lifecycle")
    public Optional<ProtocolSubmissionResult> submitAndSynchronize(Processo processo,
                                                                   ProceduralSubmissionBlueprintReport blueprint,
                                                                   ProceduralConnectorExecutionReport execution,
                                                                   boolean autoProtocolRequested) {
        Optional<ProtocolSubmissionResult> result = submissionService.submitIfEligible(processo, blueprint, execution, autoProtocolRequested);
        result.ifPresent(item -> {
            submissionService.applySubmissionResult(processo, item);
            synchronizeExternalState(processo, item);
        });
        return result;
    }

    public void synchronizeExternalState(Processo processo, ProtocolSubmissionResult result) {
        if (processo == null) {
            return;
        }
        processo.setConnectorSyncAttempts((processo.getConnectorSyncAttempts() == null ? 0 : processo.getConnectorSyncAttempts()) + 1);
        if (result == null) {
            processo.setConnectorSyncStatus(firstNonBlank(processo.getConnectorSyncStatus(), "SYNC_NOT_EXECUTED"));
            return;
        }
        if (!result.accepted() || result.system() == null || isBlank(processo.getNumeroUnificado())) {
            processo.setConnectorSyncStatus(firstNonBlank(processo.getConnectorSyncStatus(), "SYNC_SKIPPED"));
            processo.setConnectorSyncMessage(truncate(firstNonBlank(result.message(), "Sincronização externa não executada após a tentativa de protocolo."), 500));
            return;
        }
        Instant startedAt = Instant.now();
        try {
            Optional<ExternalProcessSnapshot> snapshot = processSyncService.syncSnapshot(result.system(), processo.getNumeroUnificado());
            int events = processSyncService.syncEvents(result.system(), processo.getNumeroUnificado(), startedAt.minusSeconds(21600));
            if (snapshot.isPresent()) {
                ExternalProcessSnapshot item = snapshot.orElseThrow();
                processo.setClasseProcessual(firstNonBlank(processo.getClasseProcessual(), item.classeProcessual()));
                processo.setAssunto(firstNonBlank(processo.getAssunto(), item.assunto()));
                if (processo.getNivelSigilo() == null && item.nivelSigilo() != null) {
                    processo.setNivelSigilo(item.nivelSigilo());
                }
                processo.setConnectorSnapshotSyncedAt(LocalDateTime.ofInstant(item.fetchedAt() == null ? startedAt : item.fetchedAt(), ZoneOffset.UTC));
            }
            if (events > 0) {
                processo.setConnectorEventsSyncedAt(LocalDateTime.ofInstant(startedAt, ZoneOffset.UTC));
            }
            processo.setConnectorSyncStatus(resolveSyncStatus(snapshot.isPresent(), events));
            processo.setConnectorSyncMessage(truncate("Snapshot=" + snapshot.isPresent() + "; Eventos=" + events + "; Sistema=" + result.system().name(), 500));
        } catch (Exception ex) {
            processo.setConnectorSyncStatus("SYNC_ERROR");
            processo.setConnectorSyncMessage(truncate(firstNonBlank(ex.getMessage(), "Falha na sincronização externa do protocolo judicial."), 500));
        }
    }

    private String resolveSyncStatus(boolean snapshot, int events) {
        if (snapshot && events > 0) {
            return "SNAPSHOT_AND_EVENTS_SYNCED";
        }
        if (snapshot) {
            return "SNAPSHOT_SYNCED";
        }
        if (events > 0) {
            return "EVENTS_SYNCED";
        }
        return "SYNC_DISPATCHED";
    }

    private String firstNonBlank(String... values) {
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, Math.max(0, max - 1));
    }
}
