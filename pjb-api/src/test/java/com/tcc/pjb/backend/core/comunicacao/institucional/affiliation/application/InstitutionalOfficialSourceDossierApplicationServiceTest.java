package com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliation;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalAffiliationRequestStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalAffiliationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialSourceDossier;
import com.tcc.pjb.backend.model.dto.admin.AdminInstitutionalPublicRecognitionResponse;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalAffiliationStatus;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalNominationRole;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalOrganizationScope;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalTrustLevel;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class InstitutionalOfficialSourceDossierApplicationServiceTest {

    private final InstitutionalAffiliationStateRepository affiliationRepository = mock(InstitutionalAffiliationStateRepository.class);
    private final InstitutionalAffiliationRequestStateRepository requestRepository = mock(InstitutionalAffiliationRequestStateRepository.class);
    private final InstitutionalPublicRecognitionGateApplicationService gateApplicationService = mock(InstitutionalPublicRecognitionGateApplicationService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-04-05T12:00:00Z"), ZoneOffset.UTC);
    private final InstitutionalOfficialSourceDossierApplicationService service = new InstitutionalOfficialSourceDossierApplicationService(
            affiliationRepository,
            requestRepository,
            gateApplicationService,
            clock
    );

    @Test
    void shouldBuildSovereignDossierForJudiciaryAffiliation() {
        InstitutionalAffiliation affiliation = affiliation();
        when(affiliationRepository.findByAffiliationId("aff-1")).thenReturn(java.util.Optional.of(affiliation));
        when(gateApplicationService.inspecionarAfiliacao(affiliation)).thenReturn(new InstitutionalPublicRecognitionGateApplicationService.RecognitionInput(
                "FORUM",
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                false,
                true
        ));
        when(gateApplicationService.avaliarAfiliacao(affiliation)).thenReturn(report("RECONHECIDA_AUTOMATICAMENTE", true, false, List.of(), List.of("CNJ_DATAJUD_OU_SIORG", "RECEITA_CNPJ")));

        InstitutionalOfficialSourceDossier dossier = service.gerarAfiliacao("aff-1");

        assertTrue(dossier.sovereignRecognitionReady());
        assertFalse(dossier.dueNow());
        assertEquals("RECONHECIDA_AUTOMATICAMENTE", dossier.publicRecognitionStatus());
        assertTrue(dossier.sources().stream().anyMatch(item -> item.sourceCode().equals("CNJ_DATAJUD") && item.applicable() && item.satisfied()));
        assertTrue(dossier.sources().stream().anyMatch(item -> item.sourceCode().equals("GOVBR") && item.satisfied()));
    }

    @Test
    void shouldExposeParentTrustGapForSubordinateUnitWithoutOwnCnpj() {
        InstitutionalAffiliation affiliation = new InstitutionalAffiliation(
                "aff-sub",
                DestinatarioInstitucionalKind.MINISTERIO_PUBLICO,
                "MPCE",
                "Ministério Público do Ceará",
                "PROMOTORIA-LN-01",
                "Promotoria de Limoeiro do Norte",
                InstitutionalOrganizationScope.PROMOTORIA,
                null,
                "CE",
                "Limoeiro do Norte",
                null,
                "ESTADUAL",
                List.of("INFANCIA"),
                List.of("CE:Limoeiro do Norte"),
                "mpce.mp.br",
                "Promotor-Geral",
                99L,
                InstitutionalNominationRole.ADMINISTRADOR_INSTITUCIONAL,
                "seguranca@mpce.mp.br",
                List.of("PORTAL"),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
                false,
                true,
                false,
                true,
                InstitutionalAffiliationStatus.HOMOLOGADA,
                List.of("ato_normativo_promotoria"),
                Instant.parse("2026-03-01T00:00:00Z"),
                Instant.parse("2026-03-10T00:00:00Z"),
                null
        );
        when(affiliationRepository.findByAffiliationId("aff-sub")).thenReturn(java.util.Optional.of(affiliation));
        when(gateApplicationService.inspecionarAfiliacao(affiliation)).thenReturn(new InstitutionalPublicRecognitionGateApplicationService.RecognitionInput(
                "PROMOTORIA",
                false,
                false,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                false
        ));
        when(gateApplicationService.avaliarAfiliacao(affiliation)).thenReturn(report("PENDENTE_EVIDENCIAS", false, true, List.of("instituicao_pai_nao_reconhecida"), List.of()));

        InstitutionalOfficialSourceDossier dossier = service.gerarAfiliacao("aff-sub");

        assertFalse(dossier.sovereignRecognitionReady());
        assertTrue(dossier.dueNow());
        assertTrue(dossier.blockingIssues().contains("instituicao_pai_nao_reconhecida"));
        assertTrue(dossier.sources().stream().anyMatch(item -> item.sourceCode().equals("HERANCA_DE_CONFIANCA_INSTITUCIONAL") && item.applicable() && !item.satisfied()));
    }

    private static InstitutionalAffiliation affiliation() {
        return new InstitutionalAffiliation(
                "aff-1",
                DestinatarioInstitucionalKind.ORGAO_JUDICIAL_EXTERNO,
                "TJCE",
                "Tribunal de Justiça do Ceará",
                "FORUM-LN",
                "Fórum de Limoeiro do Norte",
                InstitutionalOrganizationScope.FORUM,
                null,
                "CE",
                "Limoeiro do Norte",
                "27.000.000/0001-00",
                "ESTADUAL",
                List.of("CIVEL"),
                List.of("CE:Limoeiro do Norte"),
                "tjce.jus.br",
                "Presidência",
                10L,
                InstitutionalNominationRole.ADMINISTRADOR_INSTITUCIONAL,
                "seguranca@tjce.jus.br",
                List.of("PORTAL", "API"),
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
                List.of("base_cnj"),
                Instant.parse("2026-03-01T00:00:00Z"),
                Instant.parse("2026-04-01T00:00:00Z"),
                null
        );
    }

    private static AdminInstitutionalPublicRecognitionResponse report(String statusCode,
                                                                      boolean recognized,
                                                                      boolean humanReviewRequired,
                                                                      List<String> blockers,
                                                                      List<String> acceptedSources) {
        return new AdminInstitutionalPublicRecognitionResponse(
                "v1",
                Instant.parse("2026-04-05T12:00:00Z"),
                "FORUM",
                "Fórum",
                statusCode,
                statusCode,
                recognized,
                "RECONHECIDA_AUTOMATICAMENTE".equals(statusCode),
                humanReviewRequired,
                acceptedSources,
                List.of(
                        new AdminInstitutionalPublicRecognitionResponse.EvidenceRule("ANCORA_CATALOGO_SOBERANO", "Âncora soberana", true, acceptedSources.contains("CNJ_DATAJUD_OU_SIORG"), "CNJ_DATAJUD_OU_SIORG"),
                        new AdminInstitutionalPublicRecognitionResponse.EvidenceRule("EMAIL_INSTITUCIONAL", "E-mail oficial", true, true, "CANAL_OFICIAL"),
                        new AdminInstitutionalPublicRecognitionResponse.EvidenceRule("DOMINIO_INSTITUCIONAL", "Domínio oficial", true, true, "DNS_E_GOVERNANCA_INSTITUCIONAL"),
                        new AdminInstitutionalPublicRecognitionResponse.EvidenceRule("TOPOLOGIA_TERRITORIAL", "Topologia territorial", true, true, "IBGE_OU_TOPOLOGIA_CNJ"),
                        new AdminInstitutionalPublicRecognitionResponse.EvidenceRule("REPRESENTANTE_GOVBR_OURO", "Gov.br", true, true, "GOVBR"),
                        new AdminInstitutionalPublicRecognitionResponse.EvidenceRule("REPRESENTANTE_ICP_BRASIL", "ICP", true, true, "ITI_ICP_BRASIL")
                ),
                List.of(),
                List.of(),
                blockers,
                List.of("emitir_codigo_de_ativacao_para_canal_oficial")
        );
    }
}
