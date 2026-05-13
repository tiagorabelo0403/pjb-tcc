package com.tcc.pjb.backend.core.comunicacao.institucional.access.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalManagedCredential;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalStrongSignaturePolicy;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalTrustAssessmentApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliation;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalNomination;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalTrustAssessment;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalAffiliationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalNominationStateRepository;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalSecurityFactor;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalStrongSignaturePolicyApplicationService {

    private final CurrentUserService currentUserService;
    private final InstitutionalAffiliationStateRepository affiliationRepository;
    private final InstitutionalNominationStateRepository nominationRepository;
    private final InstitutionalManagedCredentialApplicationService managedCredentialApplicationService;
    private final InstitutionalRootAdministratorApprovalApplicationService rootAdministratorApprovalApplicationService;
    private final InstitutionalTrustAssessmentApplicationService trustAssessmentApplicationService;

    public InstitutionalStrongSignaturePolicyApplicationService(CurrentUserService currentUserService,
                                                               InstitutionalAffiliationStateRepository affiliationRepository,
                                                               InstitutionalNominationStateRepository nominationRepository,
                                                               InstitutionalManagedCredentialApplicationService managedCredentialApplicationService,
                                                               InstitutionalRootAdministratorApprovalApplicationService rootAdministratorApprovalApplicationService,
                                                               InstitutionalTrustAssessmentApplicationService trustAssessmentApplicationService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.affiliationRepository = Objects.requireNonNull(affiliationRepository);
        this.nominationRepository = Objects.requireNonNull(nominationRepository);
        this.managedCredentialApplicationService = Objects.requireNonNull(managedCredentialApplicationService);
        this.rootAdministratorApprovalApplicationService = Objects.requireNonNull(rootAdministratorApprovalApplicationService);
        this.trustAssessmentApplicationService = Objects.requireNonNull(trustAssessmentApplicationService);
    }

    public InstitutionalStrongSignaturePolicy avaliar(String affiliationId, String nominationId) {
        Usuario user = currentUserService.getRequired();
        InstitutionalNomination nomination = resolveNomination(user.getId(), affiliationId, nominationId);
        InstitutionalAffiliation affiliation = resolveAffiliation(affiliationId, nomination);
        InstitutionalTrustAssessment assessment = trustAssessmentApplicationService.avaliar(user, affiliation, nomination);
        boolean signCapability = nomination != null && nomination.capacidades() != null && nomination.capacidades().stream().anyMatch(cap -> cap.isAtoDeAssinaturaOuManifestacao())
                || nomination != null && nomination.funcaoOperacional() != null && nomination.funcaoOperacional().isFuncaoAssinantePreferencial();
        boolean managedCredentialActive = nomination != null && managedCredentialApplicationService.listar(affiliation == null ? affiliationId : affiliation.affiliationId()).stream()
                .filter(InstitutionalManagedCredential::ativa)
                .anyMatch(item -> Objects.equals(item.nominationId(), nomination.nominationId()));
        boolean govBrSatisfied = assessment.factors().contains(InstitutionalSecurityFactor.LOGIN_GOVBR);
        boolean govBrPrataOuroSatisfied = assessment.factors().contains(InstitutionalSecurityFactor.GOVBR_PRATA_OURO);
        boolean certificateSatisfied = assessment.factors().contains(InstitutionalSecurityFactor.CERTIFICADO_ICP_BRASIL);
        boolean networkSatisfied = assessment.certificadoPermitidoNaSessao();
        boolean mfaSatisfied = assessment.factors().contains(InstitutionalSecurityFactor.MFA_ATIVO);
        boolean rootApprovalRequired = affiliation != null && affiliation.requerDuplaAprovacaoAdministrador();
        boolean rootApprovalSatisfied = rootAdministratorApprovalApplicationService.isSatisfied(affiliation == null ? affiliationId : affiliation.affiliationId());
        ArrayList<String> findings = new ArrayList<>();
        if (signCapability && managedCredentialActive) {
            findings.add("credencial_gerenciada_ativa_nao_substitui_assinatura_forte");
        }
        if (!govBrSatisfied) findings.add("identidade_govbr_raiz_ausente");
        if (!govBrPrataOuroSatisfied) findings.add("govbr_prata_ou_ouro_ausente");
        if (signCapability && !certificateSatisfied) findings.add("certificado_qualificado_ausente");
        if (signCapability && !networkSatisfied) findings.add("rede_institucional_ou_autorizacao_remota_nao_confirmada");
        if (signCapability && !mfaSatisfied) findings.add("mfa_stepup_ausente");
        if (rootApprovalRequired && !rootApprovalSatisfied) findings.add("aprovacao_admin_raiz_pendente");
        boolean allowed = !signCapability || (govBrSatisfied && govBrPrataOuroSatisfied && certificateSatisfied && networkSatisfied && mfaSatisfied && (!rootApprovalRequired || rootApprovalSatisfied));
        return new InstitutionalStrongSignaturePolicy(
                affiliation == null ? affiliationId : affiliation.affiliationId(),
                nomination == null ? nominationId : nomination.nominationId(),
                user.getId(),
                user.getNome(),
                nomination == null || nomination.accessLaneKind() == null ? null : nomination.accessLaneKind().name(),
                signCapability,
                managedCredentialActive,
                true,
                govBrSatisfied,
                true,
                govBrPrataOuroSatisfied,
                signCapability,
                certificateSatisfied,
                signCapability,
                networkSatisfied,
                signCapability,
                mfaSatisfied,
                rootApprovalRequired,
                rootApprovalSatisfied,
                allowed,
                List.copyOf(findings),
                List.of(
                        "assinatura_forte_reusa_governanca_de_confianca_institucional",
                        "identidade_pessoal_govbr_nao_e_substituida_por_login_gerenciado",
                        "atos_de_assinatura_e_peticionamento_exigem_certificado_qualificado_e_stepup"),
                Instant.now()
        );
    }

    private InstitutionalNomination resolveNomination(Long currentUserId, String affiliationId, String nominationId) {
        if (nominationId != null && !nominationId.isBlank()) {
            return nominationRepository.findByNominationId(nominationId).orElse(null);
        }
        return nominationRepository.findByNominatedUserId(currentUserId).stream()
                .filter(item -> affiliationId == null || affiliationId.isBlank() || affiliationId.equals(item.affiliationId()))
                .sorted(Comparator.comparing(InstitutionalNomination::updatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .findFirst()
                .orElse(null);
    }

    private InstitutionalAffiliation resolveAffiliation(String affiliationId, InstitutionalNomination nomination) {
        String resolved = affiliationId == null || affiliationId.isBlank() ? nomination == null ? null : nomination.affiliationId() : affiliationId;
        return resolved == null ? null : affiliationRepository.findByAffiliationId(resolved).orElse(null);
    }
}
