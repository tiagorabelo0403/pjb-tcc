package com.tcc.pjb.backend.service.casefile;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityConsistencyResponse;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityIntegrationResponse;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityObservabilityResponse;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityReadinessResponse;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityRemediationResponse;

@Service
public class CaseContinuityRemediationService {

    private final CaseContinuityObservabilityService observabilityService;
    private final CaseContinuityConsistencyService consistencyService;
    private final CaseContinuityReadinessService readinessService;
    private final CaseContinuityIntegrationService integrationService;
    private final AuditLedgerService auditLedgerService;
    private final CaseContinuityObservabilityMetrics metrics;

    public CaseContinuityRemediationService(CaseContinuityObservabilityService observabilityService,
                                           CaseContinuityConsistencyService consistencyService,
                                           CaseContinuityReadinessService readinessService,
                                           CaseContinuityIntegrationService integrationService,
                                           AuditLedgerService auditLedgerService,
                                           CaseContinuityObservabilityMetrics metrics) {
        this.observabilityService = Objects.requireNonNull(observabilityService);
        this.consistencyService = Objects.requireNonNull(consistencyService);
        this.readinessService = Objects.requireNonNull(readinessService);
        this.integrationService = Objects.requireNonNull(integrationService);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
        this.metrics = Objects.requireNonNull(metrics);
    }

    @Transactional(readOnly = true)
    public CaseContinuityRemediationResponse snapshot(Long processoId) {
        Instant generatedAt = Instant.now();
        CaseContinuityObservabilityResponse observability = observabilityService.snapshot(processoId);
        CaseContinuityConsistencyResponse consistency = consistencyService.snapshot(processoId);
        CaseContinuityReadinessResponse readiness = readinessService.snapshot(processoId);
        CaseContinuityIntegrationResponse integration = integrationService.snapshot(processoId);

        LinkedHashSet<String> warnings = new LinkedHashSet<>(observability.warnings());
        warnings.addAll(consistency.warnings());
        warnings.addAll(readiness.warnings());
        warnings.addAll(integration.warnings());

        LinkedHashSet<String> blockers = new LinkedHashSet<>(consistency.inconsistencies());
        blockers.addAll(readiness.blockers());
        blockers.addAll(integration.blockers());

        LinkedHashSet<String> automatedRepairActions = new LinkedHashSet<>();
        LinkedHashSet<String> manualRepairActions = new LinkedHashSet<>();
        LinkedHashSet<String> recommendedActions = new LinkedHashSet<>(consistency.recommendedActions());
        recommendedActions.addAll(readiness.recommendedActions());
        recommendedActions.addAll(integration.recommendedActions());

        if (observability.staleProceedings() > 0) {
            automatedRepairActions.add("Sincronizar proceedings defasados do caso raiz antes do próximo ato sensível.");
        }
        if (consistency.orphanParentCount() > 0) {
            automatedRepairActions.add("Reconciliar parentProceedingKey órfão e recompor a hierarquia estrutural do caso unificado.");
        }
        if (consistency.incompatibleRoleTrackCount() > 0 || consistency.incompatibleStateCount() > 0) {
            automatedRepairActions.add("Recalcular role, track, fase e status derivados dos proceedings materializados.");
        }
        if (readiness.expectedTrack() != null && readiness.dominantTrack() != readiness.expectedTrack()) {
            automatedRepairActions.add("Reconciliar track dominante do caso com o track esperado pelo lifecycle processual.");
        }
        if (!integration.lifecycleConnected()) {
            automatedRepairActions.add("Repropagar a malha de ações do lifecycle e recalcular permissões do rito ativo.");
        }
        if (!integration.securityConnected()) {
            automatedRepairActions.add("Recarregar o catálogo canônico de atos sensíveis e os vínculos de step-up e binding.");
        }
        if (consistency.recursalBranchesWithoutEdge() > 0) {
            automatedRepairActions.add("Materializar edges ausentes nas ramificações recursais já reconhecidas no caso raiz.");
        }
        if (consistency.executoryBranchesWithoutParent() > 0) {
            automatedRepairActions.add("Reconectar ramificações executórias ao parentProceedingKey estruturalmente correto.");
        }

        if (!observability.unifiedRoot() || consistency.rootProceedingCount() != 1) {
            manualRepairActions.add("Saneamento institucional do proceeding raiz e consolidação definitiva do case file principal.");
        }
        if (!integration.structuredContinuation()) {
            manualRepairActions.add("Consolidar manualmente a continuidade estrutural entre conhecimento, recurso, cumprimento e execução.");
        }
        if (!integration.recursalMatrixReady()) {
            manualRepairActions.add("Expandir a malha recursal/catalogal para cobrir integralmente as espécies ainda não resolvidas.");
        }
        if (!integration.financialAiReady()) {
            manualRepairActions.add("Revisar o selector e o descriptor consolidados do Financial AI antes de depender da IA em fluxos críticos.");
        }
        if (consistency.proceedingCount() == 0) {
            manualRepairActions.add("Reconstituir a materialização mínima do caso unificado, pois nenhum proceeding estrutural foi encontrado.");
        }
        if (observability.shadowProceedings() > 0) {
            manualRepairActions.add("Eliminar shadow proceedings e consolidar a leitura operacional do caso raiz em um único eixo consistente.");
        }

        if (observability.dominantTrack() != null && observability.dominantTrack().requiresRemediationSweep()) {
            recommendedActions.add("Executar varredura de continuidade ampliada antes de liberar o próximo salto de fase ou ato terminal.");
        }
        if (!consistency.healthy()) {
            recommendedActions.add("Submeter o caso a saneamento estrutural antes de publicar, transitar, executar ou arquivar.");
        }
        if (!readiness.healthy()) {
            recommendedActions.add("Reconciliar readiness e lifecycle antes de permitir novos atos críticos no mesmo organismo processual.");
        }

        boolean autoRepairEligible = manualRepairActions.isEmpty();
        long totalIssues = blockers.size() + automatedRepairActions.size() + manualRepairActions.size();
        boolean healthy = blockers.isEmpty() && consistency.healthy() && readiness.healthy() && integration.healthy() && manualRepairActions.isEmpty();

        CaseContinuityRemediationResponse response = new CaseContinuityRemediationResponse(
                generatedAt,
                observability.caseFileId(),
                processoId,
                observability.dominantTrack(),
                readiness.expectedTrack(),
                readiness.readinessLevel(),
                healthy,
                autoRepairEligible,
                totalIssues,
                automatedRepairActions.size(),
                manualRepairActions.size(),
                java.util.List.copyOf(automatedRepairActions),
                java.util.List.copyOf(manualRepairActions),
                java.util.List.copyOf(warnings),
                java.util.List.copyOf(blockers),
                java.util.List.copyOf(recommendedActions)
        );
        metrics.recordRemediation(response);
        auditLedgerService.appendSafely(
                "CASE_CONTINUITY_REMEDIATION_INSPECT",
                "CASE_FILE",
                String.valueOf(observability.caseFileId()),
                String.join("|",
                        String.valueOf(observability.caseFileId()),
                        String.valueOf(processoId),
                        response.autoRepairEligible() ? "AUTO" : "MANUAL",
                        String.valueOf(response.totalIssues()),
                        String.valueOf(response.manualActionCount()))
        );
        return response;
    }
}
