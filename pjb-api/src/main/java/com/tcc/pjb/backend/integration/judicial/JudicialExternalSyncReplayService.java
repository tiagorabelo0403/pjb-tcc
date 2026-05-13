package com.tcc.pjb.backend.integration.judicial;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class JudicialExternalSyncReplayService {

    private static final List<String> RETRYABLE_SYNC_STATUSES = List.of(
            "SYNC_ERROR",
            "SYNC_SKIPPED",
            "SYNC_DISPATCHED"
    );

    private final ProcessoRepository processoRepository;
    private final JudicialConnectorLifecycleService judicialConnectorLifecycleService;

    public JudicialExternalSyncReplayService(ProcessoRepository processoRepository,
                                             JudicialConnectorLifecycleService judicialConnectorLifecycleService) {
        this.processoRepository = processoRepository;
        this.judicialConnectorLifecycleService = judicialConnectorLifecycleService;
    }

    @Transactional
    public int replayPendingSynchronizations(int batchSize, int maxAttempts) {
        int safeBatch = Math.max(1, batchSize);
        int safeMaxAttempts = Math.max(1, maxAttempts);
        List<Processo> candidates = processoRepository.findConnectorSyncReplayCandidates(RETRYABLE_SYNC_STATUSES, safeMaxAttempts, PageRequest.of(0, safeBatch)).getContent();
        int synced = 0;
        for (Processo processo : candidates) {
            if (processo != null && attemptSync(processo)) {
                synced++;
            }
        }
        return synced;
    }

    @Transactional
    public boolean attemptSync(Processo processo) {
        if (processo == null) {
            return false;
        }
        JudicialSystem system = parseSystem(processo.getConnectorSystem());
        if (system == null || processo.getConnectorProtocolReference() == null || processo.getConnectorProtocolReference().isBlank()) {
            processo.setConnectorSyncStatus("SYNC_REPLAY_SKIPPED");
            processoRepository.save(processo);
            return false;
        }
        try {
            ProtocolSubmissionResult synthetic = new ProtocolSubmissionResult(
                    true,
                    system,
                    processo.getConnectorProtocolReference(),
                    processo.getConnectorSubmissionStatus(),
                    processo.getConnectorSubmissionMessage(),
                    Instant.now(),
                    java.util.Map.of("mode", "SYNC_REPLAY")
            );
            judicialConnectorLifecycleService.synchronizeExternalState(processo, synthetic);
            processoRepository.save(processo);
            return processo.getConnectorSyncStatus() != null && processo.getConnectorSyncStatus().contains("SYNCED");
        } catch (Exception ex) {
            log.warn("Falha ao reexecutar sincronização externa do processo id={}: {}", processo.getId(), ex.getMessage());
            processo.setConnectorSyncStatus("SYNC_REPLAY_ERROR");
            processo.setConnectorSyncMessage(truncate(ex.getMessage(), 500));
            processoRepository.save(processo);
            return false;
        }
    }

    private JudicialSystem parseSystem(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return JudicialSystem.valueOf(raw.trim().toUpperCase());
        } catch (Exception ignored) {
            return null;
        }
    }

    private String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, Math.max(0, max - 1));
    }
}
