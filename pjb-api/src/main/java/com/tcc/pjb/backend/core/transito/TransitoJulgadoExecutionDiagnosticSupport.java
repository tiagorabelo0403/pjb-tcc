package com.tcc.pjb.backend.core.transito;

import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleAction;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class TransitoJulgadoExecutionDiagnosticSupport {

    private final PostJudgmentOperationalResolver operationalResolver;
    private final WorkItemRepository workItemRepository;
    private final TransitoJulgadoNarrativeSupport narrativeSupport;
    private final ExecutionMeshStateService executionMeshStateService;

    public TransitoJulgadoExecutionDiagnosticSupport(
            PostJudgmentOperationalResolver operationalResolver,
            WorkItemRepository workItemRepository,
            TransitoJulgadoNarrativeSupport narrativeSupport,
            ExecutionMeshStateService executionMeshStateService
    ) {
        this.operationalResolver = operationalResolver;
        this.workItemRepository = workItemRepository;
        this.narrativeSupport = narrativeSupport;
        this.executionMeshStateService = executionMeshStateService;
    }

    public Map<String, Object> diagnosticarMalhaExecutiva(Long processoId, Processo processo, boolean executionReady) {
        PostJudgmentOperationalProfile operationalProfile = operationalResolver.resolve(
                processo,
                ProcessoLifecycleAction.INICIAR_CUMPRIMENTO,
                processo.getResultadoFinal(),
                0D);
        long pendentes = processo.getId() == null ? 0L : workItemRepository.countOpenByProcesso(processo.getId());
        long bloqueantes = processo.getId() == null ? 0L : workItemRepository.countOpenBlockingByProcesso(processo.getId());

        LinkedHashMap<String, Object> incidentMatrix = narrativeSupport.buildIncidentMatrix(processo);
        LinkedHashMap<String, Object> actMatrix = narrativeSupport.buildActMatrix(processo);
        LinkedHashMap<String, Object> patrimonialMatrix = narrativeSupport.buildPatrimonialMatrix(processo);
        LinkedHashMap<String, Object> externalConstrictionMatrix = narrativeSupport.buildExternalConstrictionMatrix(processo);
        LinkedHashMap<String, Object> expropriationMatrix = narrativeSupport.buildExpropriationMatrix(processo);
        LinkedHashMap<String, Object> auctionCycleMatrix = narrativeSupport.buildAuctionCycleMatrix(processo);
        LinkedHashMap<String, Object> contingencyMatrix = narrativeSupport.buildContingencyMatrix(processo);
        LinkedHashMap<String, Object> reconciliationMatrix = narrativeSupport.buildReconciliationMatrix(processo);
        LinkedHashMap<String, Object> homologationMatrix = narrativeSupport.buildHomologationMatrix(processo);
        LinkedHashMap<String, Object> settlementMatrix = narrativeSupport.buildSettlementMatrix(processo);
        LinkedHashMap<String, Object> closureGovernanceMatrix = narrativeSupport.buildClosureGovernanceMatrix(processo);
        LinkedHashMap<String, Object> terminalMatrix = narrativeSupport.buildTerminalMatrix(processo);
        LinkedHashMap<String, Object> archiveLinkMatrix = narrativeSupport.buildArchiveLinkMatrix(processo);
        Map<String, Object> snapshotExecutivo = executionMeshStateService.readSnapshot(
                processo,
                operationalProfile,
                incidentMatrix,
                actMatrix,
                patrimonialMatrix,
                externalConstrictionMatrix,
                expropriationMatrix,
                auctionCycleMatrix,
                contingencyMatrix,
                reconciliationMatrix,
                homologationMatrix,
                settlementMatrix,
                closureGovernanceMatrix,
                terminalMatrix,
                archiveLinkMatrix);

        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("processoId", processoId);
        out.put("numero", processo.getNumeroProcesso());
        out.put("statusAtual", processo.getStatusProcesso() != null ? processo.getStatusProcesso().name() : null);
        out.put("faseAtual", processo.getFaseAtual() != null ? processo.getFaseAtual().name() : null);
        out.put("executionReady", executionReady);
        out.put("pendenciasOperacionais", pendentes);
        out.put("bloqueiosOperacionais", bloqueantes);
        out.put("operationalDescriptor", operationalProfile.descriptor());
        out.put("operationalProfile", operationalProfile.toMap());
        out.put("incidentMatrix", incidentMatrix);
        out.put("actMatrix", actMatrix);
        out.put("patrimonialMatrix", patrimonialMatrix);
        out.put("externalConstrictionMatrix", externalConstrictionMatrix);
        out.put("expropriationMatrix", expropriationMatrix);
        out.put("auctionCycleMatrix", auctionCycleMatrix);
        out.put("contingencyMatrix", contingencyMatrix);
        out.put("reconciliationMatrix", reconciliationMatrix);
        out.put("homologationMatrix", homologationMatrix);
        out.put("settlementMatrix", settlementMatrix);
        out.put("closureGovernanceMatrix", closureGovernanceMatrix);
        out.put("terminalMatrix", terminalMatrix);
        out.put("archiveLinkMatrix", archiveLinkMatrix);
        out.put("snapshotExecutivo", snapshotExecutivo);
        return out;
    }

    public Map<String, Object> consultarSnapshotExecutivo(Processo processo) {
        PostJudgmentOperationalProfile operationalProfile = operationalResolver.resolve(
                processo,
                ProcessoLifecycleAction.INICIAR_CUMPRIMENTO,
                processo.getResultadoFinal(),
                0D);
        return executionMeshStateService.readSnapshot(
                processo,
                operationalProfile,
                narrativeSupport.buildIncidentMatrix(processo),
                narrativeSupport.buildActMatrix(processo),
                narrativeSupport.buildPatrimonialMatrix(processo),
                narrativeSupport.buildExternalConstrictionMatrix(processo),
                narrativeSupport.buildExpropriationMatrix(processo),
                narrativeSupport.buildAuctionCycleMatrix(processo),
                narrativeSupport.buildContingencyMatrix(processo),
                narrativeSupport.buildReconciliationMatrix(processo),
                narrativeSupport.buildHomologationMatrix(processo),
                narrativeSupport.buildSettlementMatrix(processo),
                narrativeSupport.buildClosureGovernanceMatrix(processo),
                narrativeSupport.buildTerminalMatrix(processo),
                narrativeSupport.buildArchiveLinkMatrix(processo));
    }
}
