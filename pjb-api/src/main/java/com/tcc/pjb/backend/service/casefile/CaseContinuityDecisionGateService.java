package com.tcc.pjb.backend.service.casefile;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleAction;
import com.tcc.pjb.backend.core.processual.ato.AtoProcessualDescriptor;
import com.tcc.pjb.backend.core.processual.ato.AtoProcessualSecurityPolicyService;
import com.tcc.pjb.backend.core.processual.ato.AtoProcessualSecurityProfile;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityDecisionGateResponse;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityIntegrationResponse;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityReadinessResponse;

@Service
public class CaseContinuityDecisionGateService {

    private final CaseContinuityReadinessService readinessService;
    private final CaseContinuityIntegrationService integrationService;
    private final AtoProcessualSecurityPolicyService securityPolicyService;
    private final AuditLedgerService auditLedgerService;
    private final CaseContinuityObservabilityMetrics metrics;

    public CaseContinuityDecisionGateService(CaseContinuityReadinessService readinessService,
                                             CaseContinuityIntegrationService integrationService,
                                             AtoProcessualSecurityPolicyService securityPolicyService,
                                             AuditLedgerService auditLedgerService,
                                             CaseContinuityObservabilityMetrics metrics) {
        this.readinessService = Objects.requireNonNull(readinessService);
        this.integrationService = Objects.requireNonNull(integrationService);
        this.securityPolicyService = Objects.requireNonNull(securityPolicyService);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
        this.metrics = Objects.requireNonNull(metrics);
    }

    @Transactional(readOnly = true)
    public CaseContinuityDecisionGateResponse snapshot(Long processoId, ProcessoLifecycleAction action) {
        Instant generatedAt = Instant.now();
        CaseContinuityReadinessResponse readiness = readinessService.snapshot(processoId);
        CaseContinuityIntegrationResponse integration = integrationService.snapshot(processoId);
        AtoProcessualDescriptor descriptor = securityPolicyService.descriptorForAction(action);
        String canonicalActType = descriptor != null && descriptor.codigo() != null
                ? descriptor.codigo()
                : securityPolicyService.canonicalActType(action.name());
        AtoProcessualSecurityProfile profile = descriptor != null && descriptor.securityProfile() != null
                ? descriptor.securityProfile()
                : securityPolicyService.securityProfileForActType(canonicalActType);
        boolean lifecycleAllowsAction = readiness.allowedActions().stream().anyMatch(item -> item.startsWith(action.name() + "["));
        boolean lifecycleExplicitlyBlocks = readiness.blockedActions().stream().anyMatch(item -> item.startsWith(action.name() + "["));
        boolean sensitive = action.isSensitiveJudicial() || profile.requiresElevatedSecurity() || profile.requiresCrossCheck() || profile.requiresHumanReason();

        LinkedHashSet<String> warnings = new LinkedHashSet<>(readiness.warnings());
        warnings.addAll(integration.warnings());
        LinkedHashSet<String> blockers = new LinkedHashSet<>();
        LinkedHashSet<String> recommendedActions = new LinkedHashSet<>(readiness.recommendedActions());
        recommendedActions.addAll(integration.recommendedActions());

        if (lifecycleExplicitlyBlocks || !lifecycleAllowsAction) {
            blockers.add("O lifecycle processual ainda não liberou a ação " + action.name() + " para o estado materializado do processo.");
            recommendedActions.add("Reconciliar lifecycle, fase e status do processo antes de insistir no ato " + canonicalActType + '.');
        }
        if (readiness.readinessLevel().isCritical()) {
            blockers.add("A prontidão operacional do caso unificado está em nível crítico para o ato solicitado.");
        }
        if (!integration.healthy()) {
            blockers.addAll(integration.blockers());
        }
        if (sensitive && !integration.securityConnected()) {
            blockers.add("A superfície de segurança reforçada ainda não está completamente conectada para o ato sensível " + canonicalActType + '.');
            recommendedActions.add("Revalidar binding, step-up e conferência cruzada antes do ato sensível " + canonicalActType + '.');
        }
        if (action.isRecursal() && !integration.recursalMatrixReady()) {
            blockers.add("A malha recursal ainda não está totalmente pronta para suportar o ato " + action.name() + " no contexto atual.");
        }
        if (sensitive && !integration.financialAiReady() && action == ProcessoLifecycleAction.INICIAR_CUMPRIMENTO) {
            warnings.add("O ato executório sensível será praticado sem superfície financeira consolidada plenamente pronta.");
        }
        if (!action.requiresCaseContinuityGate()) {
            warnings.add("A ação solicitada não exige gate estrutural máximo, mas foi inspecionada em superfície reforçada.");
        }

        boolean allowed = blockers.isEmpty();
        CaseContinuityDecisionGateResponse response = new CaseContinuityDecisionGateResponse(
                generatedAt,
                readiness.caseFileId(),
                processoId,
                action,
                canonicalActType,
                readiness.dominantTrack(),
                readiness.expectedTrack(),
                readiness.readinessLevel(),
                allowed,
                sensitive,
                lifecycleAllowsAction,
                integration.securityConnected(),
                integration.healthy(),
                integration.recursalMatrixReady(),
                integration.financialAiReady(),
                List.copyOf(warnings),
                List.copyOf(blockers),
                List.copyOf(recommendedActions)
        );
        metrics.recordDecisionGate(response);
        auditLedgerService.appendSafely(
                "CASE_CONTINUITY_DECISION_GATE",
                "CASE_FILE",
                String.valueOf(response.caseFileId()),
                String.join("|",
                        String.valueOf(response.caseFileId()),
                        String.valueOf(processoId),
                        action.name(),
                        response.allowed() ? "ALLOW" : "BLOCK",
                        String.valueOf(response.blockers().size()))
        );
        return response;
    }

    @Transactional(readOnly = true)
    public CaseContinuityDecisionGateResponse requireAllowed(Long processoId, ProcessoLifecycleAction action) {
        CaseContinuityDecisionGateResponse response = snapshot(processoId, action);
        if (!response.allowed()) {
            throw new IllegalStateException("Gate estrutural bloqueou o ato " + action.name() + ": " + String.join(" | ", response.blockers()));
        }
        return response;
    }
}
