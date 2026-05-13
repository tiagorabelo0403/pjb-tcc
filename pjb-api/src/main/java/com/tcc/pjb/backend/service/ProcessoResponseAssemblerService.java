package com.tcc.pjb.backend.service;

import java.util.LinkedHashSet;
import java.util.List;
import com.tcc.pjb.backend.core.procedural.NationalProceduralRoutingService;
import com.tcc.pjb.backend.core.procedural.ProceduralRoutingReport;
import com.tcc.pjb.backend.core.procedural.ProceduralSubmissionBlueprintService;
import com.tcc.pjb.backend.core.procedural.ProceduralConnectorExecutionService;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.kernel.advisory.ProcessMaterialDossierReport;
import com.tcc.pjb.backend.core.kernel.advisory.ProcessMaterialDossierService;
import com.tcc.pjb.backend.core.kernel.advisory.ProcessMaterialStrategyReport;
import com.tcc.pjb.backend.core.kernel.advisory.ProcessMaterialStrategyService;
import com.tcc.pjb.backend.mapper.ProcessoMapper;
import com.tcc.pjb.backend.model.dto.ProcessoRequest;
import com.tcc.pjb.backend.model.dto.ProcessoResponse;
import com.tcc.pjb.backend.model.entity.Processo;

@Service
public class ProcessoResponseAssemblerService {

    private final ProcessoMapper processoMapper;
    private final ProcessMaterialDossierService processMaterialDossierService;
    private final ProcessMaterialStrategyService processMaterialStrategyService;
    private final NationalProceduralRoutingService nationalProceduralRoutingService;
    private final ProceduralSubmissionBlueprintService proceduralSubmissionBlueprintService;
    private final ProceduralConnectorExecutionService proceduralConnectorExecutionService;

    public ProcessoResponseAssemblerService(ProcessoMapper processoMapper,
                                            ProcessMaterialDossierService processMaterialDossierService,
                                            ProcessMaterialStrategyService processMaterialStrategyService,
                                            NationalProceduralRoutingService nationalProceduralRoutingService,
                                            ProceduralSubmissionBlueprintService proceduralSubmissionBlueprintService,
                                            ProceduralConnectorExecutionService proceduralConnectorExecutionService) {
        this.processoMapper = processoMapper;
        this.processMaterialDossierService = processMaterialDossierService;
        this.processMaterialStrategyService = processMaterialStrategyService;
        this.nationalProceduralRoutingService = nationalProceduralRoutingService;
        this.proceduralSubmissionBlueprintService = proceduralSubmissionBlueprintService;
        this.proceduralConnectorExecutionService = proceduralConnectorExecutionService;
    }

    public ProcessoResponse toResponse(Processo processo) {
        return toResponse(processo, null);
    }

    public ProcessoResponse toResponse(Processo processo, ProcessoRequest request) {
        ProcessoResponse response = processoMapper.toResponse(processo);
        List<String> signals = buildSignals(processo);
        response.setConnectorSubmissionStatus(processo.getConnectorSubmissionStatus());
        response.setConnectorProtocolReference(processo.getConnectorProtocolReference());
        response.setConnectorSubmissionMessage(processo.getConnectorSubmissionMessage());
        response.setConnectorSubmissionProcessedAt(processo.getConnectorSubmissionProcessedAt());
        response.setConnectorSubmissionAttempts(processo.getConnectorSubmissionAttempts());
        response.setConnectorLastSubmissionAttemptAt(processo.getConnectorLastSubmissionAttemptAt());
        response.setConnectorSyncStatus(processo.getConnectorSyncStatus());
        response.setConnectorSyncMessage(processo.getConnectorSyncMessage());
        response.setConnectorSnapshotSyncedAt(processo.getConnectorSnapshotSyncedAt());
        response.setConnectorEventsSyncedAt(processo.getConnectorEventsSyncedAt());
        response.setConnectorSyncAttempts(processo.getConnectorSyncAttempts());
        ProcessMaterialDossierReport dossier = processMaterialDossierService.analyzeProcess(processo, signals);
        ProcessMaterialStrategyReport strategy = processMaterialStrategyService.analyzeProcess(processo, dossier, signals);
        if (dossier != null) {
            response.setClassificacaoProbatoria(dossier.evidentiaryBracket());
            response.setClassificacaoNegocial(dossier.negotiationBracket());
        }
        ProceduralRoutingReport proceduralRouting = nationalProceduralRoutingService.analyzeProcess(processo, request);
        response.setProceduralRouting(proceduralRouting);
        var submissionBlueprint = proceduralSubmissionBlueprintService.analyzeProcess(processo, proceduralRouting);
        response.setSubmissionBlueprint(submissionBlueprint);
        response.setConnectorExecution(proceduralConnectorExecutionService.analyzeProcess(processo, proceduralRouting, submissionBlueprint));
        if (strategy != null) {
            response.setEstrategiaContenciosa(strategy.litigationPosture());
            response.setProntidaoProtocolar(strategy.protocolReadiness());
            response.setPosturaNegocial(strategy.negotiationStance());
            response.setMaturidadeProbatoria(strategy.evidenceReadiness());
            response.setGapsEstrategicos(mergeUnique(strategy.protocolBlockers(), strategy.evidenceAgenda(), dossier != null ? dossier.proofGaps() : null));
            response.setPlanoEstrutural(mergeUnique(strategy.pleadingBlueprint(), strategy.executionChecklist(), strategy.controlPoints(), dossier != null ? dossier.protocolChecklist() : null));
        } else if (dossier != null) {
            response.setGapsEstrategicos(mergeUnique(dossier.proofGaps()));
            response.setPlanoEstrutural(mergeUnique(dossier.protocolChecklist(), dossier.petitionSections(), dossier.thesisVectors()));
        }
        return response;
    }

    @SafeVarargs
    private static List<String> mergeUnique(List<String>... groups) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (groups != null) {
            for (List<String> group : groups) {
                if (group == null) {
                    continue;
                }
                for (String item : group) {
                    if (item != null && !item.isBlank()) {
                        out.add(item.trim());
                    }
                }
            }
        }
        return List.copyOf(out);
    }

    private static List<String> buildSignals(Processo processo) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (processo == null) {
            return List.of();
        }
        add(out, processo.getAssunto());
        add(out, processo.getObjetoProcessual());
        add(out, processo.getPedidoPrincipal());
        add(out, processo.getJanelaAcordoResumo());
        if (processo.getRamoDireito() != null) {
            out.add("Ramo consolidado: " + processo.getRamoDireito().name());
        }
        if (processo.getRito() != null) {
            out.add("Rito consolidado: " + processo.getRito().name());
        }
        if (processo.getMaterialProbatorioScore() != null) {
            out.add("Score probatório atual: " + processo.getMaterialProbatorioScore());
        }
        if (processo.getPotencialAcordoScore() != null) {
            out.add("Score negocial atual: " + processo.getPotencialAcordoScore());
        }
        add(out, processo.getConnectorSubmissionStatus());
        add(out, processo.getConnectorProtocolReference());
        add(out, processo.getConnectorSyncStatus());
        add(out, processo.getConnectorSyncMessage());
        return List.copyOf(out);
    }

    private static void add(LinkedHashSet<String> out, String value) {
        if (value != null && !value.isBlank()) {
            out.add(value.trim());
        }
    }
}
