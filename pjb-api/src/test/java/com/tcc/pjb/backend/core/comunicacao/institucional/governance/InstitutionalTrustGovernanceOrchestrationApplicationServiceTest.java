package com.tcc.pjb.backend.core.comunicacao.institucional.governance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliation;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalNomination;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalAffiliationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalNominationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.application.InstitutionalEntryContextApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntryLandingPanel;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalProcessProfile;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalTrustGovernanceOrchestrationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalJudiciaryPopulationSizing;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalTrustApprovalDecision;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalTrustGovernanceProfile;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure.InstitutionalTrustApprovalDecisionStateRepository;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.FuncaoOperacionalInstitucional;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalAccessLaneKind;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalAffiliationStatus;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalNominationRole;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalNominationStatus;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalOrganizationScope;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalTrustApprovalKind;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalTrustLevel;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class InstitutionalTrustGovernanceOrchestrationApplicationServiceTest {

    @Test
    void forumQueueMutationMustRequirePjbDirectorAndMagistrateApproval() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        InstitutionalAffiliationStateRepository affiliationRepository = mock(InstitutionalAffiliationStateRepository.class);
        InstitutionalNominationStateRepository nominationRepository = mock(InstitutionalNominationStateRepository.class);
        InstitutionalEntryContextApplicationService entryContextApplicationService = mock(InstitutionalEntryContextApplicationService.class);
        InstitutionalTrustApprovalDecisionStateRepository decisionRepository = mock(InstitutionalTrustApprovalDecisionStateRepository.class);
        InstitutionalTrustGovernanceOrchestrationApplicationService service = new InstitutionalTrustGovernanceOrchestrationApplicationService(
                currentUserService, affiliationRepository, nominationRepository, entryContextApplicationService, decisionRepository);

        Usuario servidor = usuario(10L, "Servidor Fórum", TipoUsuario.SERVIDOR_FORUM);
        InstitutionalAffiliation affiliation = afiliacaoForum("AFF-1");
        InstitutionalNomination nomination = nomeacao("NOM-1", TipoUsuario.SERVIDOR_FORUM, EnumSet.of(CapacidadeCaixaInstitucional.REDISTRIBUIR_INTERNAMENTE));

        when(currentUserService.getRequired()).thenReturn(servidor);
        when(nominationRepository.findByNominationId("NOM-1")).thenReturn(Optional.of(nomination));
        when(affiliationRepository.findByAffiliationId("AFF-1")).thenReturn(Optional.of(affiliation));
        when(decisionRepository.findByProfileKey("AFF-1|NOM-1")).thenReturn(List.of(
                decisao("AFF-1|NOM-1", InstitutionalTrustApprovalKind.PJB),
                decisao("AFF-1|NOM-1", InstitutionalTrustApprovalKind.DIRETOR_GERAL)
        ));

        InstitutionalTrustGovernanceProfile profile = service.avaliarAtual("AFF-1", "NOM-1");

        assertThat(profile.requiredApprovals()).containsExactly("PJB", "DIRETOR_GERAL", "MAGISTRADO_REFERENCIAL");
        assertThat(profile.pendingApprovals()).containsExactly("MAGISTRADO_REFERENCIAL");
        assertThat(profile.fullyApproved()).isFalse();
        assertThat(profile.landingPath()).contains("/api/v1/institucional");
        assertThat(profile.horizontalDataPlaneKey()).isEqualTo("FORUM_CE");
    }

    @Test
    void magistrateProfileMustRequireOnlyPjbAndDirector() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        InstitutionalAffiliationStateRepository affiliationRepository = mock(InstitutionalAffiliationStateRepository.class);
        InstitutionalNominationStateRepository nominationRepository = mock(InstitutionalNominationStateRepository.class);
        InstitutionalEntryContextApplicationService entryContextApplicationService = mock(InstitutionalEntryContextApplicationService.class);
        InstitutionalTrustApprovalDecisionStateRepository decisionRepository = mock(InstitutionalTrustApprovalDecisionStateRepository.class);
        InstitutionalTrustGovernanceOrchestrationApplicationService service = new InstitutionalTrustGovernanceOrchestrationApplicationService(
                currentUserService, affiliationRepository, nominationRepository, entryContextApplicationService, decisionRepository);

        Usuario juiz = usuario(20L, "Juiz Titular", TipoUsuario.JUIZ_ESTADUAL);
        InstitutionalAffiliation affiliation = afiliacaoForum("AFF-2");
        InstitutionalNomination nomination = nomeacao("NOM-2", TipoUsuario.JUIZ_ESTADUAL, EnumSet.noneOf(CapacidadeCaixaInstitucional.class));

        when(currentUserService.getRequired()).thenReturn(juiz);
        when(nominationRepository.findByNominationId("NOM-2")).thenReturn(Optional.of(nomination));
        when(affiliationRepository.findByAffiliationId("AFF-2")).thenReturn(Optional.of(affiliation));
        when(decisionRepository.findByProfileKey("AFF-2|NOM-2")).thenReturn(List.of(
                decisao("AFF-2|NOM-2", InstitutionalTrustApprovalKind.PJB),
                decisao("AFF-2|NOM-2", InstitutionalTrustApprovalKind.DIRETOR_GERAL)
        ));

        InstitutionalTrustGovernanceProfile profile = service.avaliarAtual("AFF-2", "NOM-2");

        assertThat(profile.requiredApprovals()).containsExactly("PJB", "DIRETOR_GERAL");
        assertThat(profile.pendingApprovals()).isEmpty();
        assertThat(profile.fullyApproved()).isTrue();
        assertThat(profile.readyForInstitutionalPanel()).isTrue();
        assertThat(profile.routeToPersonalPanel()).isFalse();
    }

    @Test
    void populationSizingMustExposeBaselineAndHorizontalPlanning() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        InstitutionalAffiliationStateRepository affiliationRepository = mock(InstitutionalAffiliationStateRepository.class);
        InstitutionalNominationStateRepository nominationRepository = mock(InstitutionalNominationStateRepository.class);
        InstitutionalEntryContextApplicationService entryContextApplicationService = mock(InstitutionalEntryContextApplicationService.class);
        InstitutionalTrustApprovalDecisionStateRepository decisionRepository = mock(InstitutionalTrustApprovalDecisionStateRepository.class);
        InstitutionalTrustGovernanceOrchestrationApplicationService service = new InstitutionalTrustGovernanceOrchestrationApplicationService(
                currentUserService, affiliationRepository, nominationRepository, entryContextApplicationService, decisionRepository);

        when(affiliationRepository.findAll()).thenReturn(List.of(afiliacaoForum("AFF-1")));
        when(nominationRepository.findAll()).thenReturn(List.of(nomeacao("NOM-1", TipoUsuario.SERVIDOR_FORUM, EnumSet.of(CapacidadeCaixaInstitucional.VISUALIZAR))));

        InstitutionalJudiciaryPopulationSizing sizing = service.dimensionarUsuariosInternos();

        assertThat(sizing.tribunaisNacionais()).isEqualTo(91);
        assertThat(sizing.magistradosAtivosBaseline()).isEqualTo(18_748);
        assertThat(sizing.servidoresAtivosBaseline()).isEqualTo(278_826);
        assertThat(sizing.usuariosInternosCoreBaseline()).isEqualTo(297_574);
        assertThat(sizing.eixosParticionamento()).containsExactly("UF", "TRIBUNAL_OU_ORGAO", "UNIDADE", "CAIXA");
        assertThat(sizing.replicasLeituraRegionaisMinimas()).isGreaterThanOrEqualTo(5);
    }

    private static Usuario usuario(Long id, String nome, TipoUsuario tipoUsuario) {
        Usuario item = new Usuario();
        item.setId(id);
        item.setNome(nome);
        item.setTipoUsuario(tipoUsuario);
        item.setAtivo(true);
        return item;
    }

    private static InstitutionalAffiliation afiliacaoForum(String affiliationId) {
        return new InstitutionalAffiliation(
                affiliationId,
                DestinatarioInstitucionalKind.ORGAO_JUDICIAL_EXTERNO,
                "TJCE",
                "Tribunal de Justiça do Ceará",
                "FOR-001",
                "Fórum Central",
                InstitutionalOrganizationScope.FORUM,
                "FORUM_PADRAO",
                "CE",
                "Fortaleza",
                "00.000.000/0001-00",
                "ESTADUAL",
                List.of("CIVEL", "CRIMINAL"),
                List.of("FORTALEZA"),
                "tjce.jus.br",
                "Diretor do Fórum",
                1L,
                InstitutionalNominationRole.DIRETORIA_FORUM,
                "seguranca@tjce.jus.br",
                List.of("PUSH", "PORTAL"),
                List.of("CIENCIA_PESSOAL"),
                List.of("SLA_24H"),
                List.of("FALLBACK_COMARCA_SEDE"),
                List.of("PDPJ"),
                InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
                true,
                true,
                true,
                true,
                InstitutionalAffiliationStatus.HOMOLOGADA,
                List.of("afiliacao_homologada"),
                Instant.now().minusSeconds(3600),
                Instant.now(),
                null);
    }

    private static InstitutionalNomination nomeacao(String nominationId, TipoUsuario tipoUsuario, EnumSet<CapacidadeCaixaInstitucional> capacidades) {
        Instant now = Instant.now();
        return new InstitutionalNomination(
                nominationId,
                nominationId.equals("NOM-2") ? "AFF-2" : "AFF-1",
                nominationId.equals("NOM-2") ? 20L : 10L,
                nominationId.equals("NOM-2") ? "Juiz Titular" : "Servidor Fórum",
                tipoUsuario,
                nominationId.equals("NOM-2") ? InstitutionalAccessLaneKind.TITULAR : InstitutionalAccessLaneKind.SECRETARIA,
                nominationId.equals("NOM-2") ? InstitutionalNominationRole.TITULAR_INSTITUCIONAL : InstitutionalNominationRole.SECRETARIA_FORUM,
                nominationId.equals("NOM-2") ? FuncaoOperacionalInstitucional.MEMBRO_TITULAR : FuncaoOperacionalInstitucional.GESTOR_CAIXA,
                InstitutionalProcessProfile.SECRETARIA_FORUM,
                "FOR-001",
                "CX-001",
                capacidades,
                InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
                InstitutionalEntryLandingPanel.PAINEL_ORGAO,
                InstitutionalNominationStatus.ATIVA,
                now.minusSeconds(3600),
                now.plusSeconds(3600),
                true,
                nominationId.equals("NOM-2"),
                true,
                false,
                null,
                now.minusSeconds(3600),
                now);
    }

    private static InstitutionalTrustApprovalDecision decisao(String profileKey, InstitutionalTrustApprovalKind kind) {
        return new InstitutionalTrustApprovalDecision(
                kind.name() + "-1",
                profileKey,
                profileKey.startsWith("AFF-2") ? "AFF-2" : "AFF-1",
                profileKey.endsWith("NOM-2") ? "NOM-2" : "NOM-1",
                profileKey.endsWith("NOM-2") ? 20L : 10L,
                kind,
                999L,
                "Governança",
                true,
                List.of("approved"),
                Instant.now().minusSeconds(120),
                null);
    }
}
