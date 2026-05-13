package com.tcc.pjb.backend.service.casefile;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleAction;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityDecisionGateResponse;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityIntegrationResponse;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityProductionSealLevel;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityProductionSealResponse;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityReadinessResponse;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityRemediationResponse;

@Service
public class CaseContinuityProductionSealService {

    private static final List<ProcessoLifecycleAction> CORE_ACTIONS = List.of(
            ProcessoLifecycleAction.ASSINAR_DESPACHO,
            ProcessoLifecycleAction.PROFERIR_SENTENCA,
            ProcessoLifecycleAction.PROFERIR_VOTO,
            ProcessoLifecycleAction.LAVRAR_ACORDAO,
            ProcessoLifecycleAction.CERTIFICAR_TRANSITO,
            ProcessoLifecycleAction.INICIAR_CUMPRIMENTO,
            ProcessoLifecycleAction.ARQUIVAR
    );

    private final CaseContinuityReadinessService readinessService;
    private final CaseContinuityIntegrationService integrationService;
    private final CaseContinuityRemediationService remediationService;
    private final CaseContinuityDecisionGateService decisionGateService;
    private final AuditLedgerService auditLedgerService;
    private final CaseContinuityObservabilityMetrics metrics;

    public CaseContinuityProductionSealService(CaseContinuityReadinessService readinessService,
                                               CaseContinuityIntegrationService integrationService,
                                               CaseContinuityRemediationService remediationService,
                                               CaseContinuityDecisionGateService decisionGateService,
                                               AuditLedgerService auditLedgerService,
                                               CaseContinuityObservabilityMetrics metrics) {
        this.readinessService = Objects.requireNonNull(readinessService);
        this.integrationService = Objects.requireNonNull(integrationService);
        this.remediationService = Objects.requireNonNull(remediationService);
        this.decisionGateService = Objects.requireNonNull(decisionGateService);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
        this.metrics = Objects.requireNonNull(metrics);
    }

    @Transactional(readOnly = true)
    public CaseContinuityProductionSealResponse snapshot(Long processoId) {
        Instant generatedAt = Instant.now();
        CaseContinuityReadinessResponse readiness = readinessService.snapshot(processoId);
        CaseContinuityIntegrationResponse integration = integrationService.snapshot(processoId);
        CaseContinuityRemediationResponse remediation = remediationService.snapshot(processoId);

        LinkedHashSet<String> warnings = new LinkedHashSet<>(readiness.warnings());
        warnings.addAll(integration.warnings());
        warnings.addAll(remediation.warnings());
        LinkedHashSet<String> blockers = new LinkedHashSet<>(readiness.blockers());
        blockers.addAll(integration.blockers());
        blockers.addAll(remediation.blockers());
        LinkedHashSet<String> recommendedActions = new LinkedHashSet<>(readiness.recommendedActions());
        recommendedActions.addAll(integration.recommendedActions());
        recommendedActions.addAll(remediation.recommendedActions());
        LinkedHashSet<String> releaseCriteria = new LinkedHashSet<>();
        LinkedHashSet<String> auditedActions = new LinkedHashSet<>();
        LinkedHashSet<String> blockedActions = new LinkedHashSet<>();

        long allowedSensitiveActions = 0L;
        long blockedSensitiveActions = 0L;
        for (ProcessoLifecycleAction action : CORE_ACTIONS) {
            if (!action.isProductionSealCritical()) {
                continue;
            }
            CaseContinuityDecisionGateResponse gate = decisionGateService.snapshot(processoId, action);
            auditedActions.add(action.name() + '=' + (gate.allowed() ? "ALLOW" : "BLOCK"));
            if (gate.allowed()) {
                allowedSensitiveActions++;
                releaseCriteria.add("Gate estrutural liberou a ação crítica " + action.name() + '.');
            } else {
                blockedSensitiveActions++;
                blockedActions.add(action.name());
                blockers.addAll(gate.blockers());
                warnings.addAll(gate.warnings());
                recommendedActions.addAll(gate.recommendedActions());
            }
        }

        if (integration.lifecycleConnected()) {
            releaseCriteria.add("Lifecycle processual conectado ao caso unificado.");
        }
        if (integration.securityConnected()) {
            releaseCriteria.add("Superfície de segurança sensível conectada ao caso unificado.");
        }
        if (integration.recursalMatrixReady()) {
            releaseCriteria.add("Malha recursal materializada para o contexto principal do processo.");
        }
        if (integration.financialAiReady()) {
            releaseCriteria.add("Financial AI consolidado disponível para leitura operacional avançada.");
        }
        if (integration.structuredContinuation()) {
            releaseCriteria.add("Continuidade estrutural materializada entre proceedings, edges e eventos do caso raiz.");
        }
        if (remediation.autoRepairEligible()) {
            releaseCriteria.add("Pendências remanescentes admitem reparo automatizado sem saneamento manual estrutural.");
        }

        CaseContinuityProductionSealLevel sealLevel;
        if (blockedSensitiveActions > 0
                || readiness.readinessLevel().isCritical()
                || !integration.lifecycleConnected()
                || !integration.securityConnected()
                || !integration.recursalMatrixReady()) {
            sealLevel = CaseContinuityProductionSealLevel.BLOQUEADO;
        } else if (!warnings.isEmpty()
                || !remediation.autoRepairEligible()
                || !integration.financialAiReady()
                || !integration.structuredContinuation()
                || !readiness.readinessLevel().allowsConditionalRelease()) {
            sealLevel = CaseContinuityProductionSealLevel.CONDICIONAL;
        } else {
            sealLevel = CaseContinuityProductionSealLevel.APTO;
        }

        if (sealLevel.isBlocked()) {
            recommendedActions.add("Bloquear novos atos críticos até saneamento do gate estrutural, da malha recursal e da continuidade do caso.");
        } else if (sealLevel == CaseContinuityProductionSealLevel.CONDICIONAL) {
            recommendedActions.add("Liberar operação apenas com supervisão reforçada e rechecagem do gate antes do ato sensível seguinte.");
        } else {
            recommendedActions.add("O organismo processual está apto para operação assistida pela espinha de continuidade unificada.");
        }

        boolean healthy = sealLevel == CaseContinuityProductionSealLevel.APTO && blockers.isEmpty();
        CaseContinuityProductionSealResponse response = new CaseContinuityProductionSealResponse(
                generatedAt,
                integration.caseFileId(),
                processoId,
                integration.dominantTrack(),
                readiness.expectedTrack(),
                readiness.readinessLevel(),
                sealLevel,
                healthy,
                auditedActions.size(),
                allowedSensitiveActions,
                blockedSensitiveActions,
                integration.lifecycleConnected(),
                integration.securityConnected(),
                integration.recursalMatrixReady(),
                integration.financialAiReady(),
                integration.structuredContinuation(),
                remediation.autoRepairEligible(),
                java.util.List.copyOf(auditedActions),
                java.util.List.copyOf(blockedActions),
                java.util.List.copyOf(releaseCriteria),
                java.util.List.copyOf(warnings),
                java.util.List.copyOf(blockers),
                java.util.List.copyOf(recommendedActions)
        );
        metrics.recordProductionSeal(response);
        auditLedgerService.appendSafely(
                "CASE_CONTINUITY_PRODUCTION_SEAL",
                "CASE_FILE",
                String.valueOf(integration.caseFileId()),
                String.join("|",
                        String.valueOf(integration.caseFileId()),
                        String.valueOf(processoId),
                        response.sealLevel().name(),
                        String.valueOf(response.blockedSensitiveActions()),
                        String.valueOf(response.auditedActionCount()))
        );
        return response;
    }
}
