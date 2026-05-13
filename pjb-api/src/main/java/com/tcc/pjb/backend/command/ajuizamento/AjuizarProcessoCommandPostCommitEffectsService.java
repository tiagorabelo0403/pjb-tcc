package com.tcc.pjb.backend.command.ajuizamento;

import com.tcc.pjb.backend.ai.contract.IARequest;
import com.tcc.pjb.backend.ai.contract.IAResponse;
import com.tcc.pjb.backend.ai.orchestrator.IAOrchestrator;
import com.tcc.pjb.backend.core.compiler.LegalCompilerService;
import com.tcc.pjb.backend.core.procedural.NationalProceduralRoutingService;
import com.tcc.pjb.backend.core.procedural.ProceduralConnectorExecutionReport;
import com.tcc.pjb.backend.core.procedural.ProceduralConnectorExecutionService;
import com.tcc.pjb.backend.core.procedural.ProceduralRoutingReport;
import com.tcc.pjb.backend.core.procedural.ProceduralSubmissionBlueprintReport;
import com.tcc.pjb.backend.core.procedural.ProceduralSubmissionBlueprintService;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorLifecycleService;
import com.tcc.pjb.backend.integration.judicial.ProtocolSubmissionResult;
import com.tcc.pjb.backend.model.dto.event.ProcessoAjuizadoEvent;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.modules.auditoria.AuditoriaInteligenteService;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;
import com.tcc.pjb.backend.service.procedural.AjuizamentoCanonicalContextService;
import com.tcc.pjb.backend.service.processo.ProcessoMaterialObjetoEnrichmentService;
import com.tcc.pjb.backend.service.territorial.TerritorialProcessualService;
import com.tcc.pjb.backend.service.teto.TetoProcessualService;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class AjuizarProcessoCommandPostCommitEffectsService {

    private final ProcessoRepository processoRepository;
    private final LegalCompilerService legalCompilerService;
    private final NationalProceduralRoutingService nationalProceduralRoutingService;
    private final ProceduralSubmissionBlueprintService proceduralSubmissionBlueprintService;
    private final ProceduralConnectorExecutionService proceduralConnectorExecutionService;
    private final JudicialConnectorLifecycleService judicialConnectorLifecycleService;
    private final AuditoriaInteligenteService auditoriaInteligenteService;
    private final IAOrchestrator iaOrchestrator;
    private final ProcessoMaterialObjetoEnrichmentService materialObjetoEnrichmentService;
    private final AjuizamentoCanonicalContextService ajuizamentoCanonicalContextService;
    private final TetoProcessualService tetoProcessualService;
    private final TerritorialProcessualService territorialProcessualService;

    public AjuizarProcessoCommandPostCommitEffectsService(ProcessoRepository processoRepository,
                                                          LegalCompilerService legalCompilerService,
                                                          NationalProceduralRoutingService nationalProceduralRoutingService,
                                                          ProceduralSubmissionBlueprintService proceduralSubmissionBlueprintService,
                                                          ProceduralConnectorExecutionService proceduralConnectorExecutionService,
                                                          JudicialConnectorLifecycleService judicialConnectorLifecycleService,
                                                          AuditoriaInteligenteService auditoriaInteligenteService,
                                                          IAOrchestrator iaOrchestrator,
                                                          ProcessoMaterialObjetoEnrichmentService materialObjetoEnrichmentService,
                                                          AjuizamentoCanonicalContextService ajuizamentoCanonicalContextService,
                                                          TetoProcessualService tetoProcessualService,
                                                          TerritorialProcessualService territorialProcessualService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.legalCompilerService = Objects.requireNonNull(legalCompilerService);
        this.nationalProceduralRoutingService = Objects.requireNonNull(nationalProceduralRoutingService);
        this.proceduralSubmissionBlueprintService = Objects.requireNonNull(proceduralSubmissionBlueprintService);
        this.proceduralConnectorExecutionService = Objects.requireNonNull(proceduralConnectorExecutionService);
        this.judicialConnectorLifecycleService = Objects.requireNonNull(judicialConnectorLifecycleService);
        this.auditoriaInteligenteService = Objects.requireNonNull(auditoriaInteligenteService);
        this.iaOrchestrator = Objects.requireNonNull(iaOrchestrator);
        this.materialObjetoEnrichmentService = Objects.requireNonNull(materialObjetoEnrichmentService);
        this.ajuizamentoCanonicalContextService = Objects.requireNonNull(ajuizamentoCanonicalContextService);
        this.tetoProcessualService = Objects.requireNonNull(tetoProcessualService);
        this.territorialProcessualService = Objects.requireNonNull(territorialProcessualService);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @PjbTransactionalBudget(operation = "ajuizamento.command.post-commit.persist", maxMillis = 2600, critical = true)
    public void onProcessoAjuizado(ProcessoAjuizadoEvent event) {
        if (event == null || event.getProcessoId() == null) {
            return;
        }
        Processo processo = processoRepository.findById(event.getProcessoId()).orElse(null);
        if (processo == null) {
            return;
        }

        var compiled = legalCompilerService.compile(processo);
        ProceduralRoutingReport routing = nationalProceduralRoutingService.analyzeProcess(processo);
        ajuizamentoCanonicalContextService.consolidate(processo, compiled, routing);
        var diagnosticoTerritorial = territorialProcessualService.diagnosticar(processo, routing);
        var diagnosticoTeto = tetoProcessualService.diagnosticar(processo);
        ProceduralSubmissionBlueprintReport submissionBlueprint = proceduralSubmissionBlueprintService.analyzeProcess(processo, routing);
        ProceduralConnectorExecutionReport connectorExecution = proceduralConnectorExecutionService.analyzeProcess(processo, routing, submissionBlueprint);

        ProtocolSubmissionResult protocolSubmission = registrarProtocoloJudicialSafely(processo, submissionBlueprint, connectorExecution, event.isJuizo100Digital());
        registrarAuditoriaImutavelSafely(processo, compiled, routing, submissionBlueprint, connectorExecution, protocolSubmission, diagnosticoTerritorial, diagnosticoTeto, event);
        boolean resumoIaAtualizado = consolidarResumoIaSafely(processo);
        if (protocolSubmission != null || resumoIaAtualizado) {
            processoRepository.save(processo);
        }
    }

    private ProtocolSubmissionResult registrarProtocoloJudicialSafely(Processo processo,
                                                                      ProceduralSubmissionBlueprintReport submissionBlueprint,
                                                                      ProceduralConnectorExecutionReport connectorExecution,
                                                                      boolean juizo100Digital) {
        try {
            Optional<ProtocolSubmissionResult> protocolSubmission = judicialConnectorLifecycleService
                    .submitAndSynchronize(processo, submissionBlueprint, connectorExecution, juizo100Digital);
            if (protocolSubmission.isPresent()) {
                return protocolSubmission.orElseThrow();
            }
        } catch (Exception ex) {
            log.warn("Falha ao executar protocolo judicial real pos-commit. processoId={} erro={}", processo != null ? processo.getId() : null, ex.getMessage());
        }
        return null;
    }

    private void registrarAuditoriaImutavelSafely(Processo processo,
                                                  LegalCompilerService.CompiledProcess compiled,
                                                  ProceduralRoutingReport routing,
                                                  ProceduralSubmissionBlueprintReport submissionBlueprint,
                                                  ProceduralConnectorExecutionReport connectorExecution,
                                                  ProtocolSubmissionResult protocolSubmission,
                                                  TerritorialProcessualService.DiagnosticoTerritorialProcessual diagnosticoTerritorial,
                                                  TetoProcessualService.DiagnosticoTetoProcessual diagnosticoTeto,
                                                  ProcessoAjuizadoEvent event) {
        try {
            auditoriaInteligenteService.registrarEventoImutavel(
                    "PROCESSO_AJUIZADO",
                    processo.getId(),
                    "Ajuizamento realizado. Juízo 100% Digital: " + event.isJuizo100Digital()
                            + "; Ramo: " + compiled.getRamoDireito()
                            + "; Sigilo: " + processo.getNivelSigilo()
                            + "; Objeto: " + safeShort(processo.getObjetoProcessual(), 160)
                            + "; Pedido: " + safeShort(processo.getPedidoPrincipal(), 160)
                            + "; AcordoScore: " + (processo.getPotencialAcordoScore() != null ? processo.getPotencialAcordoScore() : 0)
                            + "; Roteamento: " + summarizeRouting(routing)
                            + "; Blueprint: " + summarizeBlueprint(submissionBlueprint)
                            + "; ConnectorExecution: " + summarizeConnectorExecution(connectorExecution)
                            + "; ConnectorSubmission: " + summarizeProtocolSubmission(protocolSubmission)
                            + "; ConnectorSync: " + summarizeConnectorSync(processo)
                            + "; Territorial: " + summarizeTerritorialGate(diagnosticoTerritorial)
                            + "; Teto: " + summarizeEconomicGate(diagnosticoTeto)
            );
        } catch (Exception ex) {
            log.warn("Falha ao registrar auditoria imutavel pos-commit. processoId={} erro={}", processo != null ? processo.getId() : null, ex.getMessage());
        }
    }

    private boolean consolidarResumoIaSafely(Processo processo) {
        try {
            String iaData = "Processo %s; classe=%s; assunto=%s; objeto=%s; pedido=%s; rito=%s; ramo=%s; sigilo=%s; material=%s".formatted(
                    processo.getNumeroUnificado(),
                    processo.getClasseProcessual(),
                    processo.getAssunto(),
                    processo.getObjetoProcessual(),
                    processo.getPedidoPrincipal(),
                    processo.getRito(),
                    processo.getRamoDireito(),
                    processo.getNivelSigilo(),
                    safeShort(processo.getMaterialProbatorioResumo(), 300)
            );

            IARequest request = IARequest.builder()
                    .origem("AJUIZAMENTO")
                    .acao("ajuizamento")
                    .payload("texto", iaData)
                    .payload("instrucoes", "Gerar resumo jurídico, alertas de rito e observações sobre prova e composição (sem inventar dados).")
                    .build();

            IAResponse ai = iaOrchestrator.processar(request);
            String textoIA = extrairTextoIA(ai);
            if (textoIA != null && !textoIA.isBlank()) {
                processo.setResumoIA(textoIA);
                materialObjetoEnrichmentService.enrich(processo);
                return true;
            }
        } catch (Exception ex) {
            log.warn("IAOrchestrator falhou no ajuizamento pos-commit. processoId={} erro={}", processo != null ? processo.getId() : null, ex.getMessage());
        }
        return false;
    }

    private String summarizeEconomicGate(TetoProcessualService.DiagnosticoTetoProcessual diagnosticoTeto) {
        if (diagnosticoTeto == null) {
            return "SEM_DIAGNOSTICO";
        }
        String status = diagnosticoTeto.bloqueante() ? "BLOQUEANTE" : diagnosticoTeto.alerta() ? "ALERTA" : "OK";
        return status
                + "|limite=" + diagnosticoTeto.limiteLegal()
                + "|valor=" + diagnosticoTeto.valorInformado()
                + "|competencia=" + diagnosticoTeto.competenciaSugerida()
                + "|rito=" + diagnosticoTeto.ritoSugerido();
    }

    private String summarizeTerritorialGate(TerritorialProcessualService.DiagnosticoTerritorialProcessual diagnosticoTerritorial) {
        if (diagnosticoTerritorial == null) {
            return "SEM_DIAGNOSTICO";
        }
        String status = diagnosticoTerritorial.bloqueante() ? "BLOQUEANTE" : diagnosticoTerritorial.alerta() ? "ALERTA" : "OK";
        return status
                + "|codigo=" + diagnosticoTerritorial.codigoDiagnostico()
                + "|modo=" + diagnosticoTerritorial.territorialMode()
                + "|comarca=" + firstNonBlank(diagnosticoTerritorial.comarcaSugerida(), diagnosticoTerritorial.comarcaInformada())
                + "|uf=" + firstNonBlank(diagnosticoTerritorial.ufSugerida(), diagnosticoTerritorial.ufInformada())
                + "|unidade=" + diagnosticoTerritorial.unidadeJudiciariaSugerida();
    }

    private String summarizeRouting(ProceduralRoutingReport routing) {
        if (routing == null) {
            return "N/A";
        }
        StringBuilder sb = new StringBuilder();
        appendSummary(sb, routing.tipoJusticaSugerida());
        appendSummary(sb, routing.ritoSugerido());
        appendSummary(sb, routing.tribunalCodigo());
        appendSummary(sb, routing.varaSugerida());
        if (routing.forumAllocation() != null) {
            appendSummary(sb, routing.forumAllocation().classeTpuCodigo());
            appendSummary(sb, routing.forumAllocation().competenciaTerritorialModo());
            appendSummary(sb, routing.forumAllocation().connectorSystem());
            appendSummary(sb, routing.forumAllocation().preProtocoloStatus());
        }
        return sb.isEmpty() ? "N/A" : sb.toString();
    }

    private String summarizeBlueprint(ProceduralSubmissionBlueprintReport submissionBlueprint) {
        if (submissionBlueprint == null) {
            return "N/A";
        }
        StringBuilder sb = new StringBuilder();
        appendSummary(sb, submissionBlueprint.blueprintStatus());
        appendSummary(sb, submissionBlueprint.tribunalCodigo());
        appendSummary(sb, submissionBlueprint.unidadeJudiciariaCodigo());
        appendSummary(sb, submissionBlueprint.judicialSystem() != null ? submissionBlueprint.judicialSystem().name() : null);
        appendSummary(sb, submissionBlueprint.dryRunStatus());
        return sb.isEmpty() ? "N/A" : sb.toString();
    }

    private String summarizeProtocolSubmission(ProtocolSubmissionResult protocolSubmission) {
        if (protocolSubmission == null) {
            return "SKIPPED";
        }
        StringBuilder sb = new StringBuilder();
        appendSummary(sb, protocolSubmission.system() != null ? protocolSubmission.system().name() : null);
        appendSummary(sb, protocolSubmission.status());
        appendSummary(sb, protocolSubmission.protocolReference());
        appendSummary(sb, safeShort(protocolSubmission.message(), 180));
        return sb.length() == 0 ? "NONE" : sb.toString();
    }

    private String summarizeConnectorSync(Processo processo) {
        if (processo == null) {
            return "N/A";
        }
        StringBuilder sb = new StringBuilder();
        appendSummary(sb, processo.getConnectorSyncStatus());
        appendSummary(sb, processo.getConnectorSyncMessage());
        appendSummary(sb, processo.getConnectorSnapshotSyncedAt() != null ? processo.getConnectorSnapshotSyncedAt().toString() : null);
        appendSummary(sb, processo.getConnectorEventsSyncedAt() != null ? processo.getConnectorEventsSyncedAt().toString() : null);
        return sb.isEmpty() ? "N/A" : sb.toString();
    }

    private String summarizeConnectorExecution(ProceduralConnectorExecutionReport connectorExecution) {
        if (connectorExecution == null) {
            return "N/A";
        }
        StringBuilder sb = new StringBuilder();
        appendSummary(sb, connectorExecution.executionMode());
        appendSummary(sb, connectorExecution.submissionLane());
        appendSummary(sb, connectorExecution.tribunalTargetKey());
        appendSummary(sb, connectorExecution.signerMode());
        appendSummary(sb, connectorExecution.retryPolicy());
        return sb.isEmpty() ? "N/A" : sb.toString();
    }

    private void appendSummary(StringBuilder sb, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (sb.length() > 0) {
            sb.append(" > ");
        }
        sb.append(value.trim());
    }

    private String extrairTextoIA(IAResponse ai) {
        if (ai == null) {
            return null;
        }
        try {
            return ai.getTexto();
        } catch (Throwable ignored) {
        }
        try {
            Object v = ai.getClass().getMethod("texto").invoke(ai);
            return v != null ? String.valueOf(v) : null;
        } catch (Throwable ignored) {
            return null;
        }
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

    private static String safeShort(String value, int max) {
        if (value == null) {
            return "";
        }
        String cleaned = value.replaceAll("\\s+", " ").trim();
        if (cleaned.length() <= max) {
            return cleaned;
        }
        return cleaned.substring(0, Math.max(0, max));
    }
}
