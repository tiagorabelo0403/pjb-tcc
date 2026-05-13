package com.tcc.pjb.backend.core.comunicacao.institucional.access.application;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalAffiliationOnboardingPlan;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalAuthenticationPolicyClosure;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalAuthenticationLanePolicy;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliation;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalAffiliationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalNominationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.closure.application.InstitutionalOperatingModelClosureApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.closure.domain.InstitutionalOperatingCoverageRoute;
import com.tcc.pjb.backend.core.comunicacao.institucional.closure.domain.InstitutionalOperatingModelClosure;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalAffiliationStatus;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalNominationRole;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalOrganizationScope;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalTrustLevel;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class InstitutionalAffiliationOnboardingPlanApplicationServiceTest {

    @Test
    void onboardingPlanMustMaterializeIdentityAndStrongSignaturePhases() {
        InstitutionalAffiliationStateRepository affiliationRepository = mock(InstitutionalAffiliationStateRepository.class);
        InstitutionalNominationStateRepository nominationRepository = mock(InstitutionalNominationStateRepository.class);
        InstitutionalOperatingModelClosureApplicationService operatingModel = mock(InstitutionalOperatingModelClosureApplicationService.class);
        InstitutionalAuthenticationPolicyClosureApplicationService authPolicy = mock(InstitutionalAuthenticationPolicyClosureApplicationService.class);
        InstitutionalAffiliationOnboardingPlanApplicationService service = new InstitutionalAffiliationOnboardingPlanApplicationService(
                affiliationRepository, nominationRepository, operatingModel, authPolicy);

        InstitutionalAffiliation affiliation = new InstitutionalAffiliation(
                "aff-2",
                DestinatarioInstitucionalKind.DELEGACIA_POLICIA_CIVIL,
                "PC-CE",
                "Polícia Civil do Ceará",
                "DEL-LIM",
                "Delegacia de Limoeiro",
                InstitutionalOrganizationScope.DELEGACIA,
                "DELEGACIA_BASE",
                "CE",
                "Limoeiro do Norte",
                null,
                "ESTADUAL",
                List.of("PENAL"),
                List.of("LIMOEIRO DO NORTE"),
                "pc.ce.gov.br",
                "Delegado titular",
                11L,
                InstitutionalNominationRole.GESTOR_DELEGACIA,
                "seg@pc.ce.gov.br",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
                true,
                true,
                false,
                true,
                InstitutionalAffiliationStatus.HOMOLOGADA,
                List.of(),
                Instant.now(),
                Instant.now(),
                null);

        when(affiliationRepository.findByAffiliationId("aff-2")).thenReturn(Optional.of(affiliation));
        when(nominationRepository.findAll()).thenReturn(List.of());
        when(operatingModel.consolidar(affiliation, List.of(), affiliation.destinatarioKind(), affiliation.comarca(), affiliation.uf())).thenReturn(
                new InstitutionalOperatingModelClosure(
                        affiliation.affiliationId(),
                        affiliation.orgaoSigla(),
                        affiliation.orgaoNome(),
                        affiliation.destinatarioKind().name(),
                        affiliation.organizationScope().name(),
                        affiliation.blueprintCode(),
                        "INSTITUCIONAL_AFILIADO",
                        true,
                        true,
                        false,
                        "SEDE_COMPETENTE_UF",
                        new InstitutionalOperatingCoverageRoute("Limoeiro do Norte", "CE", affiliation.destinatarioKind().name(), false, "DEL-RG", "Delegacia Regional", null, null, null, "SEDE_COMPETENTE_UF", List.of("SEDE_COMPETENTE_UF"), List.of()),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        Instant.now()));
        when(authPolicy.consolidar("aff-2")).thenReturn(new InstitutionalAuthenticationPolicyClosure(
                affiliation.affiliationId(), affiliation.orgaoSigla(), affiliation.orgaoNome(), affiliation.unidadeCodigo(), affiliation.organizationScope().name(), affiliation.blueprintCode(),
                true, true, true, true, true, false,
                List.of(new InstitutionalAuthenticationLanePolicy("triagem", "TRIAGEM", "TRIAGEM_ORGAO", "SERVIDOR_TRIAGEM", "GESTOR_DELEGACIA", "Triagem", true, "PRATA", true, true, true, false, false, false, false, false, List.of(), List.of())),
                List.of(), List.of(), Instant.now()));

        InstitutionalAffiliationOnboardingPlan plan = service.consolidar("aff-2");
        assertTrue(plan.steps().stream().anyMatch(item -> item.stepCode().equals("IDENTIDADE_RAIZ_GOVBR")));
        assertTrue(plan.steps().stream().anyMatch(item -> item.stepCode().equals("ASSINATURA_FORTE_E_ATOS_SENSIVEIS")));
        assertTrue(plan.findings().stream().anyMatch(item -> item.contains("sede_competente") || item.contains("cobertura_local")));
    }
}
