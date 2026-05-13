package com.tcc.pjb.backend.core.comunicacao.institucional.governance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.datasource.PjbDataSourceRoutingProperties;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliation;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalNomination;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalAffiliationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalNominationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.closure.application.InstitutionalOperatingModelClosureApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.closure.domain.InstitutionalOperatingCoverageRoute;
import com.tcc.pjb.backend.core.comunicacao.institucional.closure.domain.InstitutionalOperatingModelClosure;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.application.InstitutionalEntryContextApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntryLandingPanel;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntrySummary;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalProcessProfile;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalHorizontalDataPlaneApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalHorizontalDataPlanePlan;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalTrustApprovalDecision;
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

class InstitutionalHorizontalDataPlaneApplicationServiceTest {

    @Test
    void mustBuildGranularHorizontalPlanWithReplicaHeadersAndTrustChain() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        InstitutionalAffiliationStateRepository affiliationRepository = mock(InstitutionalAffiliationStateRepository.class);
        InstitutionalNominationStateRepository nominationRepository = mock(InstitutionalNominationStateRepository.class);
        InstitutionalEntryContextApplicationService entryContextApplicationService = mock(InstitutionalEntryContextApplicationService.class);
        InstitutionalTrustApprovalDecisionStateRepository decisionRepository = mock(InstitutionalTrustApprovalDecisionStateRepository.class);
        InstitutionalOperatingModelClosureApplicationService closureApplicationService = mock(InstitutionalOperatingModelClosureApplicationService.class);
        PjbDataSourceRoutingProperties routingProperties = new PjbDataSourceRoutingProperties();
        routingProperties.getRegionalSelection().getUfToReplica().put("CE", "read-ce-1");
        routingProperties.getRegionalSelection().getTribunalToReplica().put("TJCE", "read-tjce-a");
        InstitutionalHorizontalDataPlaneApplicationService service = new InstitutionalHorizontalDataPlaneApplicationService(
                currentUserService,
                affiliationRepository,
                nominationRepository,
                entryContextApplicationService,
                decisionRepository,
                closureApplicationService,
                routingProperties);

        Usuario servidor = usuario(10L, "Servidor Fórum", TipoUsuario.SERVIDOR_FORUM);
        InstitutionalAffiliation affiliation = afiliacaoForum("AFF-1");
        InstitutionalNomination nomination = nomeacao("NOM-1", TipoUsuario.SERVIDOR_FORUM, EnumSet.of(CapacidadeCaixaInstitucional.REDISTRIBUIR_INTERNAMENTE));
        InstitutionalOperatingModelClosure closure = new InstitutionalOperatingModelClosure(
                "AFF-1", "TJCE", "Tribunal de Justiça do Ceará", "ORGAO_JUDICIAL_EXTERNO", "FORUM", "FORUM_PADRAO", "INSTITUCIONAL", true,
                true, true, "SEDE_COMPETENTE_UF",
                new InstitutionalOperatingCoverageRoute("Morada Nova", "CE", "ORGAO_JUDICIAL_EXTERNO", false, "FOR-001", "Fórum Central", "Foro Central", "Fortaleza", "TJCE", "SEDE_COMPETENTE_UF", List.of("SEDE_COMPETENTE_UF"), List.of("fallback")),
                List.of(), List.of(), List.of(), List.of(), Instant.now());

        when(currentUserService.getRequired()).thenReturn(servidor);
        when(nominationRepository.findByNominationId("NOM-1")).thenReturn(Optional.of(nomination));
        when(affiliationRepository.findByAffiliationId("AFF-1")).thenReturn(Optional.of(affiliation));
        when(entryContextApplicationService.resolverEntradaAtual()).thenReturn(new InstitutionalEntrySummary(null, null, null, null, false, false, List.of(), null, Instant.now()));
        when(closureApplicationService.consolidar(affiliation, List.of(nomination), affiliation.destinatarioKind(), affiliation.comarca(), affiliation.uf())).thenReturn(closure);
        when(decisionRepository.findByProfileKey("AFF-1|NOM-1")).thenReturn(List.of(
                decisao("AFF-1|NOM-1", InstitutionalTrustApprovalKind.PJB),
                decisao("AFF-1|NOM-1", InstitutionalTrustApprovalKind.DIRETOR_GERAL)
        ));

        InstitutionalHorizontalDataPlanePlan plan = service.avaliarAtual("AFF-1", "NOM-1");

        assertThat(plan.coverageMode()).isEqualTo("SEDE_COMPETENTE_UF");
        assertThat(plan.responsibleTribunalCode()).isEqualTo("TJCE");
        assertThat(plan.responsibleUnitCode()).isEqualTo("FOR-001");
        assertThat(plan.readReplicaCode()).isEqualTo("read-tjce-a");
        assertThat(plan.partitionAxes()).containsExactly("UF", "TRIBUNAL_OU_ORGAO", "UNIDADE", "CAIXA");
        assertThat(plan.requiredApprovals()).containsExactly("PJB", "DIRETOR_GERAL", "MAGISTRADO_REFERENCIAL");
        assertThat(plan.pendingApprovals()).containsExactly("MAGISTRADO_REFERENCIAL");
        assertThat(plan.routingHeaders()).containsEntry("X-PJB-UF", "CE");
        assertThat(plan.routingHeaders()).containsEntry("X-PJB-Tribunal", "TJCE");
        assertThat(plan.routingHeaders()).containsEntry("X-PJB-Unidade", "FOR-001");
        assertThat(plan.routingHeaders()).containsEntry("X-PJB-Caixa", "CX-001");
        assertThat(plan.horizontalDataPlaneKey()).startsWith("CE|TJCE|FOR-001|CX-001|B");
    }

    @Test
    void magistratePlanMustStayReadyForInstitutionalPanelWithoutMagistrateReferenceApproval() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        InstitutionalAffiliationStateRepository affiliationRepository = mock(InstitutionalAffiliationStateRepository.class);
        InstitutionalNominationStateRepository nominationRepository = mock(InstitutionalNominationStateRepository.class);
        InstitutionalEntryContextApplicationService entryContextApplicationService = mock(InstitutionalEntryContextApplicationService.class);
        InstitutionalTrustApprovalDecisionStateRepository decisionRepository = mock(InstitutionalTrustApprovalDecisionStateRepository.class);
        InstitutionalOperatingModelClosureApplicationService closureApplicationService = mock(InstitutionalOperatingModelClosureApplicationService.class);
        InstitutionalHorizontalDataPlaneApplicationService service = new InstitutionalHorizontalDataPlaneApplicationService(
                currentUserService,
                affiliationRepository,
                nominationRepository,
                entryContextApplicationService,
                decisionRepository,
                closureApplicationService,
                new PjbDataSourceRoutingProperties());

        Usuario juiz = usuario(20L, "Juiz Titular", TipoUsuario.JUIZ_ESTADUAL);
        InstitutionalAffiliation affiliation = afiliacaoForum("AFF-2");
        InstitutionalNomination nomination = nomeacao("NOM-2", TipoUsuario.JUIZ_ESTADUAL, EnumSet.noneOf(CapacidadeCaixaInstitucional.class));
        InstitutionalOperatingModelClosure closure = new InstitutionalOperatingModelClosure(
                "AFF-2", "TJCE", "Tribunal de Justiça do Ceará", "ORGAO_JUDICIAL_EXTERNO", "FORUM", "FORUM_PADRAO", "INSTITUCIONAL", true,
                true, true, "LOCAL",
                new InstitutionalOperatingCoverageRoute("Fortaleza", "CE", "ORGAO_JUDICIAL_EXTERNO", true, "FOR-001", "Fórum Central", "Foro Central", "Fortaleza", "TJCE", "LOCAL", List.of("MUNICIPIO_LOCAL"), List.of("local")),
                List.of(), List.of(), List.of(), List.of(), Instant.now());

        when(currentUserService.getRequired()).thenReturn(juiz);
        when(nominationRepository.findByNominationId("NOM-2")).thenReturn(Optional.of(nomination));
        when(affiliationRepository.findByAffiliationId("AFF-2")).thenReturn(Optional.of(affiliation));
        when(entryContextApplicationService.resolverEntradaAtual()).thenReturn(new InstitutionalEntrySummary(null, null, null, null, false, false, List.of(), null, Instant.now()));
        when(closureApplicationService.consolidar(affiliation, List.of(nomination), affiliation.destinatarioKind(), affiliation.comarca(), affiliation.uf())).thenReturn(closure);
        when(decisionRepository.findByProfileKey("AFF-2|NOM-2")).thenReturn(List.of(
                decisao("AFF-2|NOM-2", InstitutionalTrustApprovalKind.PJB),
                decisao("AFF-2|NOM-2", InstitutionalTrustApprovalKind.DIRETOR_GERAL)
        ));

        InstitutionalHorizontalDataPlanePlan plan = service.avaliarAtual("AFF-2", "NOM-2");

        assertThat(plan.requiredApprovals()).containsExactly("PJB", "DIRETOR_GERAL");
        assertThat(plan.pendingApprovals()).isEmpty();
        assertThat(plan.readyForInstitutionalPanel()).isTrue();
        assertThat(plan.routeToPersonalPanel()).isFalse();
        assertThat(plan.localUnitPresent()).isTrue();
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
