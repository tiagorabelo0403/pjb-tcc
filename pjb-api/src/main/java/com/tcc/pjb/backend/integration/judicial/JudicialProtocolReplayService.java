package com.tcc.pjb.backend.integration.judicial;

import com.tcc.pjb.backend.core.procedural.NationalProceduralRoutingService;
import com.tcc.pjb.backend.core.procedural.ProceduralConnectorExecutionReport;
import com.tcc.pjb.backend.core.procedural.ProceduralConnectorExecutionService;
import com.tcc.pjb.backend.core.procedural.ProceduralRoutingReport;
import com.tcc.pjb.backend.core.procedural.ProceduralSubmissionBlueprintReport;
import com.tcc.pjb.backend.core.procedural.ProceduralSubmissionBlueprintService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class JudicialProtocolReplayService {

    private static final List<String> RETRYABLE_STATUSES = List.of(
            "CONNECTOR_UNREACHABLE",
            "CONNECTOR_ERROR",
            "NO_ENDPOINT",
            "NOT_CONFIGURED",
            "REPLAY_SKIPPED",
            "SYNC_ERROR"
    );

    private final ProcessoRepository processoRepository;
    private final NationalProceduralRoutingService nationalProceduralRoutingService;
    private final ProceduralSubmissionBlueprintService proceduralSubmissionBlueprintService;
    private final ProceduralConnectorExecutionService proceduralConnectorExecutionService;
    private final JudicialConnectorLifecycleService judicialConnectorLifecycleService;

    public JudicialProtocolReplayService(ProcessoRepository processoRepository,
                                         NationalProceduralRoutingService nationalProceduralRoutingService,
                                         ProceduralSubmissionBlueprintService proceduralSubmissionBlueprintService,
                                         ProceduralConnectorExecutionService proceduralConnectorExecutionService,
                                         JudicialConnectorLifecycleService judicialConnectorLifecycleService) {
        this.processoRepository = processoRepository;
        this.nationalProceduralRoutingService = nationalProceduralRoutingService;
        this.proceduralSubmissionBlueprintService = proceduralSubmissionBlueprintService;
        this.proceduralConnectorExecutionService = proceduralConnectorExecutionService;
        this.judicialConnectorLifecycleService = judicialConnectorLifecycleService;
    }

    @Transactional
    public int replayPendingProtocols(int batchSize, int maxAttempts) {
        int safeBatch = Math.max(1, batchSize);
        int safeMaxAttempts = Math.max(1, maxAttempts);
        List<Processo> candidates = processoRepository.findConnectorReplayCandidates(RETRYABLE_STATUSES, safeMaxAttempts, PageRequest.of(0, safeBatch)).getContent();
        int accepted = 0;
        for (Processo processo : candidates) {
            if (processo == null) {
                continue;
            }
            if (attemptReplay(processo)) {
                accepted++;
            }
        }
        return accepted;
    }

    @Transactional
    public boolean attemptReplay(Processo processo) {
        if (processo == null) {
            return false;
        }
        try {
            ProceduralRoutingReport routing = nationalProceduralRoutingService.analyzeProcess(processo);
            ProceduralSubmissionBlueprintReport blueprint = proceduralSubmissionBlueprintService.analyzeProcess(processo, routing);
            ProceduralConnectorExecutionReport execution = proceduralConnectorExecutionService.analyzeProcess(processo, routing, blueprint);
            if (routing != null && routing.forumAllocation() != null && !routing.forumAllocation().preProtocoloApto()) {
                processo.setConnectorSubmissionStatus("REPLAY_BLOCKED");
                processo.setConnectorSubmissionMessage(composeReplayMessage("Pré-protocolo permaneceu incompatível para replay automático.", routing.forumAllocation().incompatibilities()));
                processoRepository.save(processo);
                return false;
            }
            if (blueprint == null || execution == null || !blueprint.readyForRealConnectorSubmission() || !execution.allowedToAutoSubmit()) {
                processo.setConnectorSubmissionStatus("REPLAY_SKIPPED");
                processo.setConnectorSubmissionMessage(composeReplayMessage("Malha de replay ainda não apta para protocolo automático.", execution != null ? execution.blockers() : List.of()));
                processoRepository.save(processo);
                return false;
            }
            boolean accepted = judicialConnectorLifecycleService.submitAndSynchronize(processo, blueprint, execution, true)
                    .map(ProtocolSubmissionResult::accepted)
                    .orElse(false);
            processoRepository.save(processo);
            return accepted;
        } catch (Exception ex) {
            log.warn("Falha ao reexecutar protocolo judicial do processo id={}: {}", processo.getId(), ex.getMessage());
            processo.setConnectorSubmissionStatus("REPLAY_ERROR");
            processo.setConnectorSubmissionMessage(truncate(ex.getMessage(), 500));
            processoRepository.save(processo);
            return false;
        }
    }

    private String composeReplayMessage(String prefix, List<String> issues) {
        StringBuilder sb = new StringBuilder();
        if (prefix != null && !prefix.isBlank()) {
            sb.append(prefix.trim());
        }
        if (issues != null && !issues.isEmpty()) {
            for (String issue : issues) {
                if (issue == null || issue.isBlank()) {
                    continue;
                }
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(issue.trim());
            }
        }
        return truncate(sb.toString(), 500);
    }

    private String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, Math.max(0, max - 1));
    }
}
