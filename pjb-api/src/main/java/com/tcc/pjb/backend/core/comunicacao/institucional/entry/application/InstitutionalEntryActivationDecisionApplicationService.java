package com.tcc.pjb.backend.core.comunicacao.institucional.entry.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.access.application.InstitutionalStepUpAuthenticationPolicyApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalStepUpAuthenticationPolicy;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalNomination;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalNominationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntryActivationBundle;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntryActivationDecision;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntryContext;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntrySummary;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalOperationalProfileProjectionApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalSessionRiskApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOperationalProfileProjection;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.application.InstitutionalPanelProvisioningReadinessApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain.InstitutionalPanelProvisioningReadiness;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalSessionRiskAssessment;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalSessionRiskFinding;
import com.tcc.pjb.backend.core.identity.govbr.application.GovBrIdentityAssuranceApplicationService;
import com.tcc.pjb.backend.core.identity.govbr.domain.GovBrIdentityAssuranceAggregate;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalEntryMode;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalSensitiveAct;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalEntryActivationDecisionApplicationService {

    private static final String GOVBR_STEP_UP_START_PATH = "/api/v1/auth/govbr/stepup/start";

    private final CurrentUserService currentUserService;
    private final InstitutionalEntryContextApplicationService entryContextApplicationService;
    private final InstitutionalNominationStateRepository nominationRepository;
    private final InstitutionalOperationalProfileProjectionApplicationService operationalProfileProjectionApplicationService;
    private final InstitutionalSessionRiskApplicationService sessionRiskApplicationService;
    private final InstitutionalStepUpAuthenticationPolicyApplicationService stepUpAuthenticationPolicyApplicationService;
    private final GovBrIdentityAssuranceApplicationService govBrIdentityAssuranceApplicationService;
    private final InstitutionalPanelProvisioningReadinessApplicationService panelProvisioningReadinessApplicationService;

    public InstitutionalEntryActivationDecisionApplicationService(CurrentUserService currentUserService,
                                                                  InstitutionalEntryContextApplicationService entryContextApplicationService,
                                                                  InstitutionalNominationStateRepository nominationRepository,
                                                                  InstitutionalOperationalProfileProjectionApplicationService operationalProfileProjectionApplicationService,
                                                                  InstitutionalSessionRiskApplicationService sessionRiskApplicationService,
                                                                  InstitutionalStepUpAuthenticationPolicyApplicationService stepUpAuthenticationPolicyApplicationService,
                                                                  GovBrIdentityAssuranceApplicationService govBrIdentityAssuranceApplicationService,
                                                                  InstitutionalPanelProvisioningReadinessApplicationService panelProvisioningReadinessApplicationService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.entryContextApplicationService = Objects.requireNonNull(entryContextApplicationService);
        this.nominationRepository = Objects.requireNonNull(nominationRepository);
        this.operationalProfileProjectionApplicationService = Objects.requireNonNull(operationalProfileProjectionApplicationService);
        this.sessionRiskApplicationService = Objects.requireNonNull(sessionRiskApplicationService);
        this.stepUpAuthenticationPolicyApplicationService = Objects.requireNonNull(stepUpAuthenticationPolicyApplicationService);
        this.govBrIdentityAssuranceApplicationService = Objects.requireNonNull(govBrIdentityAssuranceApplicationService);
        this.panelProvisioningReadinessApplicationService = Objects.requireNonNull(panelProvisioningReadinessApplicationService);
    }

    public InstitutionalEntryActivationBundle avaliarEntradaAtual() {
        return avaliarEntradaAtual(entryContextApplicationService.resolverEntradaAtual(), null, null);
    }

    public InstitutionalEntryActivationBundle avaliarEntradaAtual(String affiliationId, String nominationId) {
        return avaliarEntradaAtual(entryContextApplicationService.resolverEntradaAtual(), affiliationId, nominationId);
    }

    public InstitutionalEntryActivationBundle avaliarEntradaAtual(InstitutionalEntrySummary summary) {
        return avaliarEntradaAtual(summary, null, null);
    }

    private InstitutionalEntryActivationBundle avaliarEntradaAtual(InstitutionalEntrySummary summary,
                                                                   String explicitAffiliationId,
                                                                   String explicitNominationId) {
        Usuario user = currentUserService.getRequired();
        InstitutionalEntrySummary safeSummary = summary == null ? entryContextApplicationService.resolverEntradaAtual() : summary;
        Instant now = Instant.now();
        InstitutionalEntryContext preferredContext = safeSummary.contextoPreferencial();
        InstitutionalNomination nomination = resolveNomination(user, preferredContext, now, explicitNominationId);
        String affiliationId = firstNonBlank(explicitAffiliationId, nomination == null ? null : nomination.affiliationId());
        String nominationId = nomination == null ? null : nomination.nominationId();
        InstitutionalOperationalProfileProjection profile = nomination == null
                ? null
                : operationalProfileProjectionApplicationService.materializar(affiliationId, nominationId);
        InstitutionalPanelProvisioningReadiness panelProvisioning = profile == null ? null : panelProvisioningReadinessApplicationService.avaliar(profile);
        InstitutionalSessionRiskAssessment riskAssessment = nomination == null && preferredContext == null
                ? null
                : sessionRiskApplicationService.avaliarAtual(affiliationId, nominationId,
                preferredContext == null ? null : preferredContext.unidadeCodigo(),
                preferredContext == null ? null : preferredContext.caixaCodigo());
        GovBrIdentityAssuranceAggregate govBrAssurance = govBrIdentityAssuranceApplicationService.atual();
        InstitutionalSensitiveAct recommendedAct = resolveRecommendedSensitiveAct(profile, nomination);
        InstitutionalStepUpAuthenticationPolicy stepUpPolicy = recommendedAct == null || nomination == null
                ? null
                : stepUpAuthenticationPolicyApplicationService.avaliarAtual(affiliationId, nominationId, recommendedAct.name());

        boolean institutionalProfileVisible = profile != null && profile.visibleInPjb();
        boolean panelProvisioningComplete = panelProvisioning != null && panelProvisioning.complete();
        boolean sharedExperienceReady = panelProvisioning != null && panelProvisioning.sharedExperienceReady();
        boolean requiresPanelProvisioningReview = institutionalProfileVisible && !panelProvisioningComplete;
        boolean directInstitutionalContextAvailable = institutionalProfileVisible
                && profile.activeNomination()
                && profile.fullyApproved()
                && profile.readyForInstitutionalPanel();
        boolean requiresManualApproval = (riskAssessment != null && riskAssessment.requiresManualApproval())
                || (stepUpPolicy != null && stepUpPolicy.requiresManualApproval())
                || (profile != null && !profile.fullyApproved());
        boolean requiresStepUp = (riskAssessment != null && riskAssessment.requiresStepUp())
                || (stepUpPolicy != null && stepUpPolicy.requiresMfa());
        boolean requiresQualifiedCertificate = stepUpPolicy != null && stepUpPolicy.requiresQualifiedCertificate();
        boolean requiresInstitutionalNetwork = stepUpPolicy != null && stepUpPolicy.requiresInstitutionalNetwork();
        boolean acceptsRemoteCertificateAuthorization = stepUpPolicy != null && stepUpPolicy.acceptsRemoteCertificateAuthorization();
        boolean requiresGovBrBinding = directInstitutionalContextAvailable
                && (requiresStepUp || requiresQualifiedCertificate)
                && (!govBrAssurance.contaGovBrVinculada() || !govBrAssurance.contextoInstitucionalFechado());
        boolean requiresTrustedDevice = directInstitutionalContextAvailable
                && (requiresStepUp || requiresQualifiedCertificate)
                && !govBrAssurance.trustedDeviceAtivo();
        boolean blocked = (riskAssessment != null && riskAssessment.blocked())
                || (stepUpPolicy != null && stepUpPolicy.blocked());
        boolean activateInstitutionalContext = directInstitutionalContextAvailable
                && panelProvisioningComplete
                && !blocked
                && !requiresManualApproval
                && !requiresPanelProvisioningReview
                && !requiresGovBrBinding
                && !requiresTrustedDevice
                && !requiresStepUp;
        boolean routeToPersonalPanel = !activateInstitutionalContext
                || (profile != null && profile.routeToPersonalPanel());
        String targetEnvironment = resolveTargetEnvironment(activateInstitutionalContext, blocked, requiresManualApproval,
                requiresPanelProvisioningReview, requiresGovBrBinding, requiresTrustedDevice, requiresStepUp);
        String entryMode = activateInstitutionalContext
                ? InstitutionalEntryMode.INSTITUCIONAL_AFILIADO.name()
                : InstitutionalEntryMode.DIRETO_PESSOA.name();

        LinkedHashSet<String> blockers = new LinkedHashSet<>();
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        LinkedHashSet<String> guarantees = new LinkedHashSet<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        fundamentos.add(InstitutionalEntryActivationMessages.PROFILE_BOUND_TO_ENTRY);
        fundamentos.add(InstitutionalEntryActivationMessages.ENTRY_DECIDED_BY_CONTEXT);
        fundamentos.add(InstitutionalEntryActivationMessages.ENTRY_REQUIRES_PJB_PROFILE);
        fundamentos.add(InstitutionalEntryActivationMessages.ENTRY_CANNOT_BYPASS_GOVERNANCE);
        fundamentos.add(InstitutionalEntryActivationMessages.entryMode(entryMode));
        fundamentos.add(InstitutionalEntryActivationMessages.targetEnvironment(targetEnvironment));
        if (profile != null) {
            fundamentos.add(InstitutionalEntryActivationMessages.profileState(profile.profileState()));
            fundamentos.add(InstitutionalEntryActivationMessages.panelCode(profile.panelCode()));
            fundamentos.add(InstitutionalEntryActivationMessages.landingPath(profile.landingPath()));
            fundamentos.add(InstitutionalEntryActivationMessages.nomination(profile.nominationId()));
            fundamentos.add(InstitutionalEntryActivationMessages.affiliation(profile.affiliationId()));
            fundamentos.add(InstitutionalEntryActivationMessages.panelProvisioning(panelProvisioningComplete));
            fundamentos.add(InstitutionalEntryActivationMessages.sharedExperience(sharedExperienceReady));
            fundamentos.addAll(profile.fundamentos());
            warnings.addAll(profile.findings());
        }
        if (panelProvisioning != null) {
            fundamentos.addAll(panelProvisioning.fundamentos());
            warnings.addAll(panelProvisioning.findings());
        }
        if (preferredContext != null) {
            fundamentos.add(InstitutionalEntryActivationMessages.context(preferredContext.contextId()));
            fundamentos.addAll(preferredContext.fundamentosEntrada());
        }
        if (safeSummary.identidadeBase() != null) {
            fundamentos.addAll(safeSummary.identidadeBase().fundamentos());
        }
        if (riskAssessment != null) {
            fundamentos.addAll(riskAssessment.fundamentos());
            for (InstitutionalSessionRiskFinding finding : riskAssessment.findings()) {
                if (finding.blocking()) {
                    blockers.add(finding.code());
                } else {
                    warnings.add(finding.code());
                }
            }
        }
        if (stepUpPolicy != null) {
            fundamentos.addAll(stepUpPolicy.fundamentos());
            if (recommendedAct != null) {
                fundamentos.add(InstitutionalEntryActivationMessages.sensitiveAct(recommendedAct.name()));
            }
            if (stepUpPolicy.blocked()) {
                blockers.addAll(stepUpPolicy.findings());
            } else {
                warnings.addAll(stepUpPolicy.findings());
            }
        }
        blockers.addAll(govBrAssurance.blockers());
        warnings.addAll(govBrAssurance.warnings());
        guarantees.addAll(govBrAssurance.garantias());
        if (institutionalProfileVisible) {
            guarantees.add("PERFIL_OPERACIONAL_VISIVEL_NO_PJB");
        }
        if (directInstitutionalContextAvailable) {
            guarantees.add("CONTEXTO_INSTITUCIONAL_MATERIALIZADO");
        }
        if (panelProvisioningComplete) {
            guarantees.add("PAINEL_COMPLETO_NO_PJB");
            fundamentos.add(InstitutionalEntryActivationMessages.PANEL_PROVISIONING_COMPLETE);
        }
        if (sharedExperienceReady) {
            guarantees.add("SUPERFICIES_COMPARTILHADAS_ATIVAS_NO_PAINEL");
            fundamentos.add(InstitutionalEntryActivationMessages.PANEL_SHARED_EXPERIENCE_READY);
        }
        if (activateInstitutionalContext) {
            guarantees.add("ATIVACAO_DIRETA_DO_PAINEL_INSTITUCIONAL_LIBERADA");
            fundamentos.add(InstitutionalEntryActivationMessages.PANEL_INSTITUTIONAL);
        } else {
            fundamentos.add(InstitutionalEntryActivationMessages.PANEL_PERSONAL);
        }
        if (blocked) {
            fundamentos.add(InstitutionalEntryActivationMessages.BLOCKED_CONTAINMENT);
        }
        if (requiresManualApproval) {
            fundamentos.add(InstitutionalEntryActivationMessages.WAITING_MANUAL_APPROVAL);
        }
        if (requiresPanelProvisioningReview) {
            fundamentos.add(InstitutionalEntryActivationMessages.WAITING_PANEL_PROVISIONING);
        }
        if (requiresGovBrBinding) {
            fundamentos.add(InstitutionalEntryActivationMessages.WAITING_GOVBR_BINDING);
        }
        if (requiresTrustedDevice) {
            fundamentos.add(InstitutionalEntryActivationMessages.WAITING_TRUSTED_DEVICE);
        }
        if (requiresStepUp) {
            fundamentos.add(InstitutionalEntryActivationMessages.WAITING_STEP_UP);
        }

        String panelCode = firstNonBlank(profile == null ? null : profile.panelCode(),
                preferredContext == null || preferredContext.landingPanel() == null ? null : preferredContext.landingPanel().name(),
                safeSummary.identidadeBase() == null || safeSummary.identidadeBase().painelBase() == null ? null : safeSummary.identidadeBase().painelBase().name());
        String landingPath = firstNonBlank(profile == null ? null : profile.landingPath(), preferredContext == null ? null : preferredContext.landingPath());
        String processAreaCode = profile == null ? null : profile.processAreaCode();
        String unidadeCodigo = firstNonBlank(profile == null ? null : profile.unidadeCodigo(), preferredContext == null ? null : preferredContext.unidadeCodigo());
        String caixaCodigo = firstNonBlank(profile == null ? null : profile.caixaCodigo(), preferredContext == null ? null : preferredContext.caixaCodigo());
        String horizontalDataPlaneKey = profile == null ? null : profile.horizontalDataPlaneKey();
        String readReplicaCode = profile == null ? null : profile.readReplicaCode();
        String riskLevel = riskAssessment == null ? "NAO_AVALIADO" : riskAssessment.riskLevel();
        int riskScore = riskAssessment == null ? 0 : riskAssessment.riskScore();
        String recommendedActCode = recommendedAct == null ? null : recommendedAct.name();
        String stepUpStartPath = requiresStepUp || requiresGovBrBinding ? GOVBR_STEP_UP_START_PATH : null;

        InstitutionalEntryActivationDecision decision = new InstitutionalEntryActivationDecision(
                user.getId(),
                user.getNome(),
                affiliationId,
                nominationId,
                profile == null ? null : profile.profileKey(),
                profile == null ? "SEM_PERFIL_OPERACIONAL" : profile.profileState(),
                targetEnvironment,
                entryMode,
                preferredContext == null ? null : preferredContext.contextId(),
                panelCode,
                landingPath,
                processAreaCode,
                unidadeCodigo,
                caixaCodigo,
                horizontalDataPlaneKey,
                readReplicaCode,
                riskLevel,
                riskScore,
                govBrAssurance.nivelGarantia(),
                recommendedActCode,
                stepUpStartPath,
                institutionalProfileVisible,
                directInstitutionalContextAvailable,
                activateInstitutionalContext,
                panelProvisioningComplete,
                sharedExperienceReady,
                requiresPanelProvisioningReview,
                routeToPersonalPanel,
                blocked,
                requiresGovBrBinding,
                requiresTrustedDevice,
                requiresStepUp,
                requiresQualifiedCertificate,
                requiresInstitutionalNetwork,
                acceptsRemoteCertificateAuthorization,
                requiresManualApproval,
                panelProvisioning == null ? List.of() : panelProvisioning.findings(),
                List.copyOf(blockers),
                List.copyOf(warnings),
                List.copyOf(guarantees),
                List.copyOf(fundamentos),
                now);
        return new InstitutionalEntryActivationBundle(profile, decision);
    }

    private InstitutionalNomination resolveNomination(Usuario user,
                                                     InstitutionalEntryContext preferredContext,
                                                     Instant now,
                                                     String explicitNominationId) {
        if (explicitNominationId != null && !explicitNominationId.isBlank()) {
            InstitutionalNomination explicit = nominationRepository.findByNominationId(explicitNominationId.trim())
                    .filter(item -> user != null && user.getId() != null && Objects.equals(item.nominatedUserId(), user.getId()))
                    .filter(item -> item.ativaEm(now))
                    .orElse(null);
            if (explicit != null) {
                return explicit;
            }
        }
        return resolveActiveNomination(user, preferredContext, now);
    }

    private InstitutionalNomination resolveActiveNomination(Usuario user,
                                                            InstitutionalEntryContext preferredContext,
                                                            Instant now) {
        if (user == null || user.getId() == null) {
            return null;
        }
        List<InstitutionalNomination> nominations = nominationRepository.findByNominatedUserId(user.getId()).stream()
                .filter(item -> item.ativaEm(now))
                .sorted(Comparator.comparing(InstitutionalNomination::updatedAt).reversed())
                .toList();
        if (nominations.isEmpty()) {
            return null;
        }
        if (preferredContext != null) {
            InstitutionalNomination exact = nominations.stream()
                    .filter(item -> equalsIgnoreCase(item.unidadeCodigo(), preferredContext.unidadeCodigo()))
                    .filter(item -> equalsIgnoreCase(item.caixaCodigo(), preferredContext.caixaCodigo()))
                    .findFirst()
                    .orElse(null);
            if (exact != null) {
                return exact;
            }
        }
        return nominations.getFirst();
    }

    private InstitutionalSensitiveAct resolveRecommendedSensitiveAct(InstitutionalOperationalProfileProjection profile,
                                                                     InstitutionalNomination nomination) {
        Set<String> capacidades = profile == null ? Set.of() : Set.copyOf(profile.capacidades());
        if (capacidade(capacidades, CapacidadeCaixaInstitucional.ASSINAR_MANIFESTACAO)) {
            return InstitutionalSensitiveAct.ASSINAR_MANIFESTACAO;
        }
        if (capacidade(capacidades, CapacidadeCaixaInstitucional.PETICIONAR_EM_NOME_DO_ORGAO)) {
            return InstitutionalSensitiveAct.PETICIONAR_EM_NOME_DO_ORGAO;
        }
        if (capacidade(capacidades, CapacidadeCaixaInstitucional.PREPARAR_MINUTA)) {
            return InstitutionalSensitiveAct.APROVAR_MINUTA_FINAL;
        }
        if (capacidade(capacidades, CapacidadeCaixaInstitucional.REDISTRIBUIR_INTERNAMENTE)) {
            return InstitutionalSensitiveAct.REDISTRIBUICAO_SENSIVEL;
        }
        if (capacidade(capacidades, CapacidadeCaixaInstitucional.GERAR_CERTIDAO_CIENCIA)) {
            return InstitutionalSensitiveAct.GERAR_CERTIDAO_DE_CIENCIA;
        }
        if (capacidade(capacidades, CapacidadeCaixaInstitucional.DAR_CIENCIA)) {
            return InstitutionalSensitiveAct.DAR_CIENCIA_INSTITUCIONAL;
        }
        if (nomination != null && nomination.requerStepUpMfa()) {
            return InstitutionalSensitiveAct.DAR_CIENCIA_INSTITUCIONAL;
        }
        return null;
    }

    private boolean capacidade(Set<String> capacidades, CapacidadeCaixaInstitucional target) {
        return target != null && capacidades.contains(target.name());
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }

    private String resolveTargetEnvironment(boolean activateInstitutionalContext,
                                            boolean blocked,
                                            boolean requiresManualApproval,
                                            boolean requiresPanelProvisioningReview,
                                            boolean requiresGovBrBinding,
                                            boolean requiresTrustedDevice,
                                            boolean requiresStepUp) {
        if (blocked) {
            return "BLOQUEADO_PARA_CONTENCAO";
        }
        if (requiresManualApproval) {
            return "AGUARDANDO_APROVACAO_MANUAL";
        }
        if (requiresPanelProvisioningReview) {
            return "AGUARDANDO_PROVISIONAMENTO_PAINEL";
        }
        if (requiresGovBrBinding) {
            return "AGUARDANDO_VINCULO_GOVBR";
        }
        if (requiresTrustedDevice) {
            return "AGUARDANDO_DISPOSITIVO_CONFIAVEL";
        }
        if (requiresStepUp) {
            return "AGUARDANDO_STEP_UP";
        }
        if (activateInstitutionalContext) {
            return "PAINEL_INSTITUCIONAL";
        }
        return "PAINEL_PESSOAL";
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
}
