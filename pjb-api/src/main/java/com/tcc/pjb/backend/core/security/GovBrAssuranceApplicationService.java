package com.tcc.pjb.backend.core.security;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.domain.GovBrAssuranceBudgetView;
import com.tcc.pjb.backend.core.security.domain.GovBrAssuranceDecisionSnapshot;
import com.tcc.pjb.backend.core.security.domain.GovBrAssuranceEnvelopeView;
import com.tcc.pjb.backend.core.security.domain.GovBrAssuranceHealthQuery;
import com.tcc.pjb.backend.core.security.domain.GovBrAssuranceHealthResult;
import com.tcc.pjb.backend.core.security.domain.GovBrAssuranceLevelResult;
import com.tcc.pjb.backend.core.security.domain.GovBrAssuranceLevelView;
import com.tcc.pjb.backend.core.security.domain.GovBrAssuranceOwnerView;
import com.tcc.pjb.backend.core.security.domain.GovBrAssurancePolicyView;
import com.tcc.pjb.backend.core.security.domain.GovBrAssuranceQuery;
import com.tcc.pjb.backend.core.security.domain.GovBrAssuranceResult;
import com.tcc.pjb.backend.core.security.domain.GovBrAssuranceSignalView;
import com.tcc.pjb.backend.core.security.domain.GovBrAssuranceStatusView;
import com.tcc.pjb.backend.core.security.domain.GovBrAssuranceTimelineResult;
import com.tcc.pjb.backend.core.security.domain.GovBrAssuranceWindowView;
import com.tcc.pjb.backend.core.security.domain.GovBrConsistencyView;
import com.tcc.pjb.backend.core.security.domain.GovBrDecisionWindowView;
import com.tcc.pjb.backend.core.security.domain.GovBrPolicyBudgetView;
import com.tcc.pjb.backend.core.security.domain.GovBrSensitiveActAssessment;
import com.tcc.pjb.backend.core.security.domain.GovBrSensitiveActAssessmentResult;
import com.tcc.pjb.backend.core.security.domain.GovBrSensitiveActHealthView;
import com.tcc.pjb.backend.core.security.domain.GovBrSensitiveActView;
import com.tcc.pjb.backend.core.security.domain.GovBrSensitiveActWindowView;
import com.tcc.pjb.backend.core.security.domain.GovBrStepUpDecisionView;
import com.tcc.pjb.backend.core.security.domain.GovBrStepUpEnvelopeView;
import com.tcc.pjb.backend.core.security.domain.GovBrStepUpHealthView;
import com.tcc.pjb.backend.core.security.domain.GovBrStepUpWindowView;
import com.tcc.pjb.backend.core.security.domain.GovBrTimelineHealthView;
import com.tcc.pjb.backend.model.dto.security.govbr.GovBrAccountEntryGovernanceResponse;
import com.tcc.pjb.backend.model.dto.security.govbr.GovBrIdentityAssuranceResponse;
import com.tcc.pjb.backend.service.security.govbr.GovBrSurfaceFacadeService;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GovBrAssuranceApplicationService {

    private final GovBrAssurancePolicy policy;
    private final GovBrSurfaceFacadeService facadeService;
    private final AuditLedgerService auditLedgerService;

    public GovBrAssuranceApplicationService(GovBrAssurancePolicy policy,
                                            GovBrSurfaceFacadeService facadeService,
                                            AuditLedgerService auditLedgerService) {
        this.policy = Objects.requireNonNull(policy);
        this.facadeService = Objects.requireNonNull(facadeService);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
    }

    @Transactional(readOnly = true)
    public GovBrAccountEntryGovernanceResponse readiness() {
        return facadeService.readiness();
    }

    @Transactional(readOnly = true)
    public GovBrIdentityAssuranceResponse identityAssurance() {
        return facadeService.identityAssurance();
    }

    @Transactional(readOnly = true)
    public GovBrAssuranceResult evaluate(String nivelAtual, boolean atoSensivel) {
        return policy.evaluate(new GovBrAssuranceQuery(normalizeLevel(nivelAtual), atoSensivel));
    }

    @Transactional(readOnly = true)
    public GovBrAssuranceLevelView level(String nivelAtual) {
        return policy.levelView(normalizeLevel(nivelAtual));
    }

    @Transactional(readOnly = true)
    public GovBrAssuranceLevelResult levelResult(String nivelAtual, boolean atoSensivel) {
        String normalized = normalizeLevel(nivelAtual);
        return new GovBrAssuranceLevelResult(
                normalized,
                policy.atoNormatizadoAtendido(normalized, true),
                policy.atoNormatizadoAtendido(normalized, false),
                policy.exigeStepUp(normalized, atoSensivel) ? "STEP_UP_REQUIRED" : "ALLOWED"
        );
    }

    @Transactional(readOnly = true)
    public GovBrAssuranceDecisionSnapshot decision(String nivelAtual, boolean atoSensivel) {
        return policy.snapshot(normalizeLevel(nivelAtual), atoSensivel);
    }

    @Transactional(readOnly = true)
    public GovBrAssuranceHealthResult health(String nivelAtual) {
        return policy.health(new GovBrAssuranceHealthQuery(normalizeLevel(nivelAtual), true));
    }

    @Transactional(readOnly = true)
    public GovBrAssuranceWindowView window(String nivelAtual, boolean atoSensivel) {
        return policy.windowView(normalizeLevel(nivelAtual), atoSensivel);
    }

    @Transactional(readOnly = true)
    public GovBrAssurancePolicyView policy() {
        return policy.policyView();
    }

    @Transactional(readOnly = true)
    public GovBrSensitiveActView sensitiveAct(String tipoAto, String nivelAtual, boolean atoSensivel) {
        return policy.sensitiveActView(new GovBrSensitiveActAssessment(normalizeText(tipoAto), normalizeLevel(nivelAtual), atoSensivel));
    }

    @Transactional(readOnly = true)
    public GovBrSensitiveActAssessmentResult sensitiveActAssessment(String tipoAto, String nivelAtual, boolean atoSensivel) {
        return policy.assess(new GovBrSensitiveActAssessment(normalizeText(tipoAto), normalizeLevel(nivelAtual), atoSensivel));
    }

    @Transactional(readOnly = true)
    public GovBrStepUpDecisionView stepUpDecision(String nivelAtual, boolean atoSensivel) {
        return policy.stepUpDecisionView(normalizeLevel(nivelAtual), atoSensivel);
    }

    @Transactional(readOnly = true)
    public GovBrAssuranceTimelineResult timeline(String nivelAtual, boolean atoSensivel) {
        GovBrAssuranceTimelineResult result = policy.timeline(normalizeLevel(nivelAtual), atoSensivel);
        auditLedgerService.appendSafely("GOVBR_ASSURANCE_TIMELINE_QUERY", "GOVBR", normalizeLevel(nivelAtual), null, "entries=" + result.entries().size());
        return result;
    }

    @Transactional(readOnly = true)
    public GovBrAssuranceStatusView status(String nivelAtual, boolean atoSensivel) {
        GovBrAssuranceResult result = evaluate(nivelAtual, atoSensivel);
        return new GovBrAssuranceStatusView(result.nivelAtual(), result.atendido() ? "ALLOWED" : "STEP_UP_REQUIRED", "requerido=" + result.nivelRequerido());
    }

    @Transactional(readOnly = true)
    public GovBrTimelineHealthView timelineHealth(String nivelAtual, boolean atoSensivel) {
        GovBrAssuranceTimelineResult result = timeline(nivelAtual, atoSensivel);
        return new GovBrTimelineHealthView(normalizeLevel(nivelAtual), result.entries().isEmpty() ? "EMPTY" : "OK", "entries=" + result.entries().size());
    }

    @Transactional(readOnly = true)
    public GovBrSensitiveActHealthView sensitiveActHealth(String tipoAto, String nivelAtual, boolean atoSensivel) {
        GovBrSensitiveActAssessmentResult result = sensitiveActAssessment(tipoAto, nivelAtual, atoSensivel);
        return new GovBrSensitiveActHealthView(normalizeText(tipoAto), result.permitido() ? "ALLOWED" : "STEP_UP_REQUIRED", "nivelRequerido=" + result.nivelRequerido());
    }

    @Transactional(readOnly = true)
    public GovBrAssuranceBudgetView budget() {
        GovBrAssurancePolicyView view = policy();
        return new GovBrAssuranceBudgetView("GOVBR_ASSURANCE", "OK", "normal=" + view.nivelMinimoNormal() + " sensivel=" + view.nivelMinimoSensivel());
    }

    @Transactional(readOnly = true)
    public GovBrPolicyBudgetView policyBudget() {
        GovBrAssurancePolicyView view = policy();
        return new GovBrPolicyBudgetView(view.nivelMinimoSensivel(), view.nivelMinimoNormal(), true);
    }

    @Transactional(readOnly = true)
    public GovBrStepUpWindowView stepUpWindow(String nivelAtual, boolean atoSensivel) {
        GovBrStepUpDecisionView view = stepUpDecision(nivelAtual, atoSensivel);
        return new GovBrStepUpWindowView(view.nivelAtual(), view.stepUpNecessario(), view.stepUpNecessario() ? "insufficient_level" : "sufficient_level", view.nivelRequerido());
    }

    @Transactional(readOnly = true)
    public GovBrDecisionWindowView decisionWindow(String nivelAtual, boolean atoSensivel) {
        GovBrStepUpDecisionView view = stepUpDecision(nivelAtual, atoSensivel);
        return new GovBrDecisionWindowView(normalizeLevel(nivelAtual), view.stepUpNecessario() ? "STEP_UP_REQUIRED" : "ALLOWED", Instant.now());
    }

    @Transactional(readOnly = true)
    public GovBrConsistencyView consistency(String nivelAtual) {
        String normalized = normalizeLevel(nivelAtual);
        boolean consistente = normalized != null && (normalized.equals("bronze") || normalized.equals("prata") || normalized.equals("ouro"));
        return new GovBrConsistencyView(normalized, consistente, consistente ? "nivel reconhecido" : "nivel nao reconhecido", "govbr-token");
    }

    @Transactional(readOnly = true)
    public GovBrAssuranceEnvelopeView envelope(String nivelAtual, boolean atoSensivel) {
        GovBrAssuranceResult result = evaluate(nivelAtual, atoSensivel);
        return new GovBrAssuranceEnvelopeView(result.nivelAtual(), result.atendido() ? "ALLOWED" : "STEP_UP_REQUIRED", "requerido=" + result.nivelRequerido(), Instant.now(), 1L);
    }

    @Transactional(readOnly = true)
    public GovBrAssuranceSignalView signal(String nivelAtual, boolean atoSensivel) {
        GovBrAssuranceResult result = evaluate(nivelAtual, atoSensivel);
        return new GovBrAssuranceSignalView(result.nivelAtual(), result.atendido() ? "ALLOWED" : "STEP_UP_REQUIRED", result.exigeStepUp() ? "step-up" : "pass-through", Instant.now(), 1L);
    }

    @Transactional(readOnly = true)
    public GovBrAssuranceOwnerView owner() {
        GovBrAccountEntryGovernanceResponse readiness = readiness();
        Long referenceId = readiness.currentUserId();
        return new GovBrAssuranceOwnerView("GOVBR_ASSURANCE", referenceId == null ? "UNBOUND" : "BOUND", referenceId == null ? "sem usuario autenticado" : "usuario=" + referenceId, Instant.now(), referenceId);
    }

    @Transactional(readOnly = true)
    public GovBrSensitiveActWindowView sensitiveActWindow(String tipoAto, boolean atoSensivel) {
        return new GovBrSensitiveActWindowView(normalizeText(tipoAto), atoSensivel ? "SENSITIVE" : "NORMAL", atoSensivel ? "ouro" : "prata", Instant.now(), 1L);
    }

    @Transactional(readOnly = true)
    public GovBrStepUpEnvelopeView stepUpEnvelope(String nivelAtual, boolean atoSensivel) {
        GovBrStepUpDecisionView view = stepUpDecision(nivelAtual, atoSensivel);
        return new GovBrStepUpEnvelopeView(normalizeLevel(nivelAtual), view.stepUpNecessario() ? "STEP_UP_REQUIRED" : "STEP_UP_NOT_REQUIRED", view.nivelRequerido(), Instant.now(), 1L);
    }

    @Transactional(readOnly = true)
    public GovBrStepUpHealthView stepUpHealth(String nivelAtual, boolean atoSensivel) {
        GovBrStepUpDecisionView view = stepUpDecision(nivelAtual, atoSensivel);
        auditLedgerService.appendSafely("GOVBR_STEPUP_HEALTH_QUERY", "GOVBR", normalizeLevel(nivelAtual), null, "required=" + view.stepUpNecessario());
        return new GovBrStepUpHealthView(normalizeLevel(nivelAtual), view.stepUpNecessario() ? "STEP_UP_REQUIRED" : "READY", Instant.now());
    }

    private String normalizeLevel(String nivelAtual) {
        if (nivelAtual == null || nivelAtual.isBlank()) {
            return null;
        }
        return nivelAtual.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return "ATO";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
