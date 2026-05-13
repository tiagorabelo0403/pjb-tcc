package com.tcc.pjb.backend.core.comunicacao.institucional.closure.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalOrganizationBlueprintCatalogApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliation;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalNomination;
import com.tcc.pjb.backend.core.comunicacao.institucional.closure.domain.InstitutionalOperatingModelClosure;
import com.tcc.pjb.backend.core.comunicacao.institucional.model.CaixaInstitucional;
import com.tcc.pjb.backend.core.comunicacao.institucional.model.CanalEntregaInstitucional;
import com.tcc.pjb.backend.core.comunicacao.institucional.model.UnidadeInstitucional;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntryLandingPanel;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalProcessProfile;
import com.tcc.pjb.backend.model.entity.enums.CanalComunicacaoInstitucional;
import com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.FuncaoOperacionalInstitucional;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalAccessLaneKind;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalAffiliationStatus;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalNominationRole;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalNominationStatus;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalOrganizationScope;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalTrustLevel;
import com.tcc.pjb.backend.model.entity.enums.PapelProcessualInstitucional;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.TipoCaixaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class InstitutionalOperatingModelClosureApplicationServiceTest {

    @Test
    void shouldFallbackMunicipalityToSeatAndKeepMagistratePersonalEntry() {
        InstitutionalOperatingModelClosureApplicationService service = new InstitutionalOperatingModelClosureApplicationService(
                new InstitutionalOrganizationBlueprintCatalogApplicationService(),
                kind -> List.of(unit("FORUM-CE-SEDE", DestinatarioInstitucionalKind.ORGAO_JUDICIAL_EXTERNO, "CE", "Fortaleza", "Fortaleza", "TJCE")));

        InstitutionalAffiliation affiliation = new InstitutionalAffiliation(
                "AFF-1",
                DestinatarioInstitucionalKind.ORGAO_JUDICIAL_EXTERNO,
                "TJCE",
                "Tribunal de Justiça do Ceará",
                "FORUM-CE-SEDE",
                "Fórum Clóvis Beviláqua",
                InstitutionalOrganizationScope.FORUM,
                "FORUM",
                "CE",
                "Fortaleza",
                "00.000.000/0001-00",
                "ESTADUAL",
                List.of("CIVIL"),
                List.of("CE"),
                "tjce.jus.br",
                "Diretoria do Foro",
                10L,
                InstitutionalNominationRole.DIRETORIA_FORUM,
                "seguranca@tjce.jus.br",
                List.of("PJB_INBOX"),
                List.of("CIENCIA_PESSOAL"),
                List.of("24H"),
                List.of("SEDE_COMPETENTE"),
                List.of("PDPJ"),
                InstitutionalTrustLevel.NIVEL_4_AMBIENTE_INSTITUCIONAL_RESTRITO,
                true,
                true,
                true,
                true,
                InstitutionalAffiliationStatus.HOMOLOGADA,
                List.of("adesao_homologada"),
                Instant.now(),
                Instant.now(),
                null);

        InstitutionalNomination judgeNomination = new InstitutionalNomination(
                "NOM-1",
                "AFF-1",
                20L,
                "Magistrado Teste",
                TipoUsuario.JUIZ,
                InstitutionalAccessLaneKind.TITULAR,
                InstitutionalNominationRole.TITULAR_INSTITUCIONAL,
                FuncaoOperacionalInstitucional.MEMBRO_TITULAR,
                InstitutionalProcessProfile.MAGISTRADO_COOPERANTE,
                "FORUM-CE-SEDE",
                "CAIXA-FORUM-CE-SEDE",
                EnumSet.of(CapacidadeCaixaInstitucional.VISUALIZAR, CapacidadeCaixaInstitucional.DAR_CIENCIA),
                InstitutionalTrustLevel.NIVEL_4_AMBIENTE_INSTITUCIONAL_RESTRITO,
                InstitutionalEntryLandingPanel.PAINEL_TITULAR,
                InstitutionalNominationStatus.ATIVA,
                Instant.now().minusSeconds(60),
                null,
                true,
                true,
                true,
                true,
                null,
                Instant.now(),
                Instant.now());

        InstitutionalOperatingModelClosure closure = service.consolidar(affiliation, List.of(judgeNomination), null, "Morada Nova", "CE");

        assertEquals("SEDE_COMPETENTE_UF", closure.coverageMode());
        assertFalse(closure.coverageRoute().localUnitPresent());
        assertTrue(closure.magistratesEnterThroughForumAndPersonalAccess());
        assertTrue(closure.roleBands().stream().anyMatch(item -> item.personalDirectEntryAllowed()));
    }

    @Test
    void shouldFlagMissingMasterAdministrationWhenAffiliationExistsWithoutAdministrativeNomination() {
        InstitutionalOperatingModelClosureApplicationService service = new InstitutionalOperatingModelClosureApplicationService(
                new InstitutionalOrganizationBlueprintCatalogApplicationService(),
                kind -> List.of(unit("MP-CE-FOR", DestinatarioInstitucionalKind.MINISTERIO_PUBLICO, "CE", "Fortaleza", "Fortaleza", "MPCE")));

        InstitutionalAffiliation affiliation = new InstitutionalAffiliation(
                "AFF-2",
                DestinatarioInstitucionalKind.MINISTERIO_PUBLICO,
                "MPCE",
                "Ministério Público do Ceará",
                "MP-CE-FOR",
                "Promotoria de Fortaleza",
                InstitutionalOrganizationScope.PROMOTORIA,
                "PROMOTORIA",
                "CE",
                "Fortaleza",
                "00.000.000/0001-01",
                "ESTADUAL",
                List.of("PENAL"),
                List.of("CE"),
                "mpce.mp.br",
                "Promotor Coordenador",
                30L,
                InstitutionalNominationRole.TITULAR_INSTITUCIONAL,
                "seguranca@mpce.mp.br",
                List.of("PJB_INBOX"),
                List.of("CIENCIA_PESSOAL"),
                List.of("24H"),
                List.of(),
                List.of("PDPJ"),
                InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
                true,
                true,
                true,
                true,
                InstitutionalAffiliationStatus.HOMOLOGADA,
                List.of("adesao_homologada"),
                Instant.now(),
                Instant.now(),
                null);

        InstitutionalOperatingModelClosure closure = service.consolidar(affiliation, List.of(), null, "Fortaleza", "CE");

        assertTrue(closure.findings().contains("gestao_mestra_institucional_sem_nomeacao_ativa"));
    }

    private UnidadeInstitucional unit(String codigo,
                                      DestinatarioInstitucionalKind kind,
                                      String uf,
                                      String comarca,
                                      String foro,
                                      String tribunal) {
        return new UnidadeInstitucional(
                codigo,
                kind,
                codigo,
                codigo,
                uf,
                comarca,
                foro,
                codigo,
                null,
                RamoDireito.CIVIL,
                GrauJurisdicao.PRIMEIRO_GRAU,
                PapelProcessualInstitucional.REPRESENTANTE_JUDICIAL_PARTE,
                new CaixaInstitucional(codigo + "-CAIXA", "Caixa", TipoCaixaInstitucional.CAIXA_UNIDADE, codigo, kind, true, false),
                List.of(new CanalEntregaInstitucional(CanalComunicacaoInstitucional.PJB_INBOX, true, false, 24, 72, null, null)),
                tribunal,
                true,
                null);
    }
}
