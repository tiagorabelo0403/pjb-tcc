package com.tcc.pjb.backend.core.comunicacao.institucional.access.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalOrganizationBlueprintCatalogApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAccessLaneBlueprint;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliation;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalOrganizationBlueprint;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalAffiliationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalNominationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntryLandingPanel;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalProcessProfile;
import com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.FuncaoOperacionalInstitucional;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalAccessLaneKind;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalAffiliationStatus;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalNominationRole;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalOrganizationScope;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalTrustLevel;
import com.tcc.pjb.backend.model.entity.enums.OrganizacaoExtraJudicialKind;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class InstitutionalAuthenticationPolicyClosureApplicationServiceTest {

    @Test
    void signerLaneMustRequireGovBrAndQualifiedCertificateTogether() {
        InstitutionalAffiliationStateRepository affiliationRepository = mock(InstitutionalAffiliationStateRepository.class);
        InstitutionalNominationStateRepository nominationRepository = mock(InstitutionalNominationStateRepository.class);
        InstitutionalOrganizationBlueprintCatalogApplicationService blueprintCatalog = mock(InstitutionalOrganizationBlueprintCatalogApplicationService.class);
        InstitutionalAuthenticationPolicyClosureApplicationService service = new InstitutionalAuthenticationPolicyClosureApplicationService(
                affiliationRepository, nominationRepository, blueprintCatalog);

        InstitutionalAffiliation affiliation = new InstitutionalAffiliation(
                "aff-1",
                DestinatarioInstitucionalKind.ORGAO_JUDICIAL_EXTERNO,
                "TJCE",
                "Tribunal de Justiça do Ceará",
                "LIM-VU",
                "Vara Única de Limoeiro",
                InstitutionalOrganizationScope.FORUM,
                "FORUM_BASE",
                "CE",
                "Limoeiro do Norte",
                null,
                "ESTADUAL",
                List.of("CIVEL"),
                List.of("LIMOEIRO DO NORTE"),
                "tjce.jus.br",
                "Diretor do Fórum",
                10L,
                InstitutionalNominationRole.DIRETORIA_FORUM,
                "seg@tjce.jus.br",
                List.of("PAINEL"),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
                true,
                true,
                true,
                true,
                InstitutionalAffiliationStatus.HOMOLOGADA,
                List.of(),
                Instant.now(),
                Instant.now(),
                null);
        InstitutionalAccessLaneBlueprint titular = new InstitutionalAccessLaneBlueprint(
                InstitutionalAccessLaneKind.TITULAR,
                "titular",
                "Titular",
                InstitutionalNominationRole.TITULAR_INSTITUCIONAL,
                FuncaoOperacionalInstitucional.MEMBRO_TITULAR,
                InstitutionalProcessProfile.MAGISTRADO_COOPERANTE,
                InstitutionalEntryLandingPanel.PAINEL_TITULAR,
                InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
                EnumSet.of(CapacidadeCaixaInstitucional.ASSINAR_MANIFESTACAO, CapacidadeCaixaInstitucional.PETICIONAR_EM_NOME_DO_ORGAO),
                true,
                true,
                true,
                true,
                List.of(),
                List.of("faixa_assinante"));
        InstitutionalOrganizationBlueprint blueprint = new InstitutionalOrganizationBlueprint(
                "FORUM_BASE",
                InstitutionalOrganizationScope.FORUM,
                "Fórum base",
                DestinatarioInstitucionalKind.ORGAO_JUDICIAL_EXTERNO,
                OrganizacaoExtraJudicialKind.COOPERACAO_JUDICIAL_EXTERNA,
                com.tcc.pjb.backend.model.entity.enums.InstitutionalEntryMode.INSTITUCIONAL_AFILIADO,
                InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
                true,
                true,
                true,
                true,
                List.of(titular),
                List.of());

        when(affiliationRepository.findByAffiliationId("aff-1")).thenReturn(Optional.of(affiliation));
        when(nominationRepository.findAll()).thenReturn(List.of());
        when(blueprintCatalog.resolve(InstitutionalOrganizationScope.FORUM, DestinatarioInstitucionalKind.ORGAO_JUDICIAL_EXTERNO)).thenReturn(Optional.of(blueprint));

        var closure = service.consolidar("aff-1");
        assertTrue(closure.dualEvidenceRequiredForSensitiveActs());
        assertEquals(1, closure.lanePolicies().size());
        var lane = closure.lanePolicies().getFirst();
        assertEquals("OURO", lane.minimumGovBrLevel());
        assertTrue(lane.requiresQualifiedCertificateForSensitiveActs());
        assertTrue(lane.signsOrSubmitsSensitiveActs());
        assertTrue(lane.requiresInstitutionalNetwork());
    }
}
