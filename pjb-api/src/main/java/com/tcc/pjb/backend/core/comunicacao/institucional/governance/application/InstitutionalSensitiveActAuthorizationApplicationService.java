package com.tcc.pjb.backend.core.comunicacao.institucional.governance.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalTrustAssessmentApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliation;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalNomination;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalTrustAssessment;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalAffiliationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalNominationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.application.InstitutionalStrongSignaturePolicyApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalSensitiveActAuthorization;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.FuncaoOperacionalInstitucional;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalNominationRole;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalSensitiveAct;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalSensitiveActAuthorizationApplicationService {

    private final CurrentUserService currentUserService;
    private final InstitutionalAffiliationStateRepository affiliationRepository;
    private final InstitutionalNominationStateRepository nominationRepository;
    private final InstitutionalTrustAssessmentApplicationService trustAssessmentService;
    private final InstitutionalSessionRiskApplicationService riskApplicationService;
    private final InstitutionalStrongSignaturePolicyApplicationService strongSignaturePolicyApplicationService;

    public InstitutionalSensitiveActAuthorizationApplicationService(CurrentUserService currentUserService,
                                                                   InstitutionalAffiliationStateRepository affiliationRepository,
                                                                   InstitutionalNominationStateRepository nominationRepository,
                                                                   InstitutionalTrustAssessmentApplicationService trustAssessmentService,
                                                                   InstitutionalSessionRiskApplicationService riskApplicationService,
                                                                   InstitutionalStrongSignaturePolicyApplicationService strongSignaturePolicyApplicationService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.affiliationRepository = Objects.requireNonNull(affiliationRepository);
        this.nominationRepository = Objects.requireNonNull(nominationRepository);
        this.trustAssessmentService = Objects.requireNonNull(trustAssessmentService);
        this.riskApplicationService = Objects.requireNonNull(riskApplicationService);
        this.strongSignaturePolicyApplicationService = Objects.requireNonNull(strongSignaturePolicyApplicationService);
    }

    public InstitutionalSensitiveActAuthorization autorizar(InstitutionalSensitiveAct act,
                                                            String affiliationId,
                                                            String nominationId) {
        Objects.requireNonNull(act, "Ato sensível é obrigatório.");
        Usuario user = currentUserService.getRequired();
        InstitutionalNomination nomination = resolveNomination(user.getId(), affiliationId, nominationId);
        InstitutionalAffiliation affiliation = resolveAffiliation(affiliationId, nomination);
        ArrayList<String> findings = new ArrayList<>();
        if (affiliation == null) {
            findings.add("afiliacao_inexistente_ou_inativa");
        }
        if (nomination == null) {
            findings.add("nomeacao_ativa_nao_localizada");
        }
        InstitutionalTrustAssessment assessment = trustAssessmentService.avaliar(user, affiliation, nomination);
        var risk = riskApplicationService.avaliarAtual(user, affiliation, nomination,
                nomination == null ? null : nomination.unidadeCodigo(), nomination == null ? null : nomination.caixaCodigo());
        var strongSignaturePolicy = strongSignaturePolicyApplicationService.avaliar(
                affiliation == null ? affiliationId : affiliation.affiliationId(),
                nomination == null ? nominationId : nomination.nominationId());
        if (nomination != null) {
            CapacidadeCaixaInstitucional requiredCapability = act.requiredCapability();
            if (requiredCapability != null && (nomination.capacidades() == null || !nomination.capacidades().contains(requiredCapability))) {
                findings.add("capacidade_obrigatoria_ausente=" + requiredCapability.name());
            }
            if (act.requireTitularAuthority() && !hasTitularAuthority(nomination)) {
                findings.add("autoridade_titular_ou_substituta_nao_confirmada");
            }
        }
        if (!assessment.trustLevel().atende(act.minimumTrust())) {
            findings.add("trust_floor_insuficiente=" + act.minimumTrust().name());
        }
        if (act.requireMfa() && !assessment.mfaAtivo()) {
            findings.add("mfa_ausente");
        }
        if (act.requireCertificate() && !assessment.factors().contains(com.tcc.pjb.backend.model.entity.enums.InstitutionalSecurityFactor.CERTIFICADO_ICP_BRASIL)) {
            findings.add("certificado_qualificado_ausente");
        }
        if (act.requireCertificate() && !assessment.factors().contains(com.tcc.pjb.backend.model.entity.enums.InstitutionalSecurityFactor.LOGIN_GOVBR)) {
            findings.add("identidade_govbr_raiz_ausente");
        }
        if (act.requireCertificate() && !assessment.factors().contains(com.tcc.pjb.backend.model.entity.enums.InstitutionalSecurityFactor.GOVBR_PRATA_OURO)) {
            findings.add("govbr_prata_ou_ouro_obrigatorio_para_ato_sensivel");
        }
        if (act.requireNetworkOrRemoteAuthorization() && !assessment.certificadoPermitidoNaSessao()) {
            findings.add("certificado_nao_permitido_na_sessao");
        }
        if (act.requireCertificate() && !strongSignaturePolicy.allowed()) {
            findings.addAll(strongSignaturePolicy.findings().stream()
                    .map(item -> "assinatura_forte=" + item)
                    .filter(item -> findings.stream().noneMatch(item::equals))
                    .toList());
        }
        if (!risk.findings().isEmpty()) {
            findings.addAll(risk.findings().stream().map(item -> "risco=" + item.code()).toList());
        }
        boolean blocked = affiliation == null
                || nomination == null
                || !assessment.autorizado()
                || risk.blocked()
                || (act.requireCertificate() && !strongSignaturePolicy.allowed())
                || findings.stream().anyMatch(item -> item.contains("ausente") || item.contains("insuficiente") || item.contains("nao_confirmada") || item.contains("nao_permitido") || item.contains("pendente"));
        boolean manual = !blocked && (risk.requiresManualApproval() || risk.requiresStepUp() || (act.requireCertificate() && !strongSignaturePolicy.rootAdministrationApprovalSatisfied()));
        boolean allowed = !blocked && !manual;
        return new InstitutionalSensitiveActAuthorization(
                UUID.randomUUID().toString(),
                act,
                user.getId(),
                user.getNome(),
                affiliation == null ? null : affiliation.affiliationId(),
                nomination == null ? null : nomination.nominationId(),
                assessment.trustLevel(),
                act.minimumTrust(),
                allowed,
                manual,
                blocked,
                List.copyOf(findings),
                List.of(
                        "ato_sensivel=" + act.name(),
                        "trust_atual=" + assessment.trustLevel().name(),
                        "trust_minimo=" + act.minimumTrust().name(),
                        "risco_nivel=" + risk.riskLevel(),
                        "assinatura_forte_permitida=" + strongSignaturePolicy.allowed(),
                        "aprovacao_admin_raiz=" + strongSignaturePolicy.rootAdministrationApprovalSatisfied()),
                Instant.now(),
                null
        );
    }

    private InstitutionalAffiliation resolveAffiliation(String affiliationId, InstitutionalNomination nomination) {
        String resolvedAffiliationId = affiliationId == null || affiliationId.isBlank()
                ? nomination == null ? null : nomination.affiliationId()
                : affiliationId;
        return resolvedAffiliationId == null ? null : affiliationRepository.findByAffiliationId(resolvedAffiliationId).orElse(null);
    }

    private InstitutionalNomination resolveNomination(Long userId, String affiliationId, String nominationId) {
        if (nominationId != null && !nominationId.isBlank()) {
            return nominationRepository.findByNominationId(nominationId).orElse(null);
        }
        Instant now = Instant.now();
        return nominationRepository.findByNominatedUserId(userId).stream()
                .filter(item -> item.ativaEm(now))
                .filter(item -> affiliationId == null || affiliationId.isBlank() || item.affiliationId().equals(affiliationId))
                .sorted(Comparator.comparing((InstitutionalNomination item) -> item.trustFloor() == null ? 0 : item.trustFloor().ordem()).reversed()
                        .thenComparing(InstitutionalNomination::updatedAt, Comparator.reverseOrder()))
                .findFirst()
                .orElse(null);
    }

    private boolean hasTitularAuthority(InstitutionalNomination nomination) {
        if (nomination == null) {
            return false;
        }
        if (nomination.nominationRole() == InstitutionalNominationRole.TITULAR_INSTITUCIONAL) {
            return true;
        }
        FuncaoOperacionalInstitucional funcao = nomination.funcaoOperacional();
        return funcao != null && funcao.isFuncaoAssinantePreferencial();
    }
}
