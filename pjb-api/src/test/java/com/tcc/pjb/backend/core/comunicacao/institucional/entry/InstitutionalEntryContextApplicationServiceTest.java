package com.tcc.pjb.backend.core.comunicacao.institucional.entry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.comunicacao.institucional.access.VinculoUsuarioCaixaInstitucional;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.VinculoUsuarioCaixaInstitucionalResolver;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalOrganizationBlueprintCatalogApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliation;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalNomination;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalAffiliationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalNominationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.application.InstitutionalEntryContextApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.application.InstitutionalIdentityBaseProfileResolverApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntryLandingPanel;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalProcessProfile;
import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.infrastructure.InstitutionalInboxStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.model.CaixaInstitucional;
import com.tcc.pjb.backend.core.comunicacao.institucional.model.CanalEntregaInstitucional;
import com.tcc.pjb.backend.core.comunicacao.institucional.model.UnidadeInstitucional;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.infrastructure.InstitutionalDelegationAssignmentStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.infrastructure.InstitutionalOperationalCoverageRuleStateRepository;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.AbrangenciaVinculoInstitucional;
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
import org.mockito.Mockito;

class InstitutionalEntryContextApplicationServiceTest {

    @Test
    void deveResolverContextoInstitucionalPorNomeacaoMesmoForaDoPerfilHumanoBase() {
        CurrentUserService currentUserService = Mockito.mock(CurrentUserService.class);
        VinculoUsuarioCaixaInstitucionalResolver vinculoResolver = Mockito.mock(VinculoUsuarioCaixaInstitucionalResolver.class);
        InstitutionalInboxStateRepository inboxRepository = Mockito.mock(InstitutionalInboxStateRepository.class);
        InstitutionalDelegationAssignmentStateRepository delegationRepository = Mockito.mock(InstitutionalDelegationAssignmentStateRepository.class);
        InstitutionalOperationalCoverageRuleStateRepository coverageRepository = Mockito.mock(InstitutionalOperationalCoverageRuleStateRepository.class);
        InstitutionalNominationStateRepository nominationRepository = Mockito.mock(InstitutionalNominationStateRepository.class);
        InstitutionalAffiliationStateRepository affiliationRepository = Mockito.mock(InstitutionalAffiliationStateRepository.class);

        InstitutionalEntryContextApplicationService service = new InstitutionalEntryContextApplicationService(
                currentUserService,
                vinculoResolver,
                inboxRepository,
                delegationRepository,
                coverageRepository,
                nominationRepository,
                affiliationRepository,
                new InstitutionalIdentityBaseProfileResolverApplicationService(),
                new InstitutionalOrganizationBlueprintCatalogApplicationService()
        );

        Usuario usuario = Usuario.builder()
                .id(7L)
                .nome("Maria Custódia")
                .email("maria@estado.ce.gov.br")
                .senha("x")
                .cpf("12345678901")
                .uf("CE")
                .comarca("Fortaleza")
                .tipoUsuario(TipoUsuario.SERVIDOR)
                .perfil(TipoUsuario.SERVIDOR.name())
                .build();

        CaixaInstitucional caixa = new CaixaInstitucional(
                "CX_CUST",
                "Custódia",
                TipoCaixaInstitucional.CAIXA_UNIDADE,
                "UP_001",
                DestinatarioInstitucionalKind.UNIDADE_PRISIONAL,
                false,
                true
        );
        UnidadeInstitucional unidade = new UnidadeInstitucional(
                "UP_001",
                DestinatarioInstitucionalKind.UNIDADE_PRISIONAL,
                "PPCE",
                "Unidade Prisional Fortaleza I",
                "CE",
                "Fortaleza",
                "Fortaleza",
                "Unidade Prisional Fortaleza I",
                null,
                RamoDireito.PENAL,
                GrauJurisdicao.PRIMEIRO_GRAU,
                PapelProcessualInstitucional.UNIDADE_EXECUTORA,
                caixa,
                List.of(new CanalEntregaInstitucional(CanalComunicacaoInstitucional.PJB_INBOX, true, false, 48, 120, null, null)),
                "TJCE",
                true,
                null
        );
        VinculoUsuarioCaixaInstitucional vinculo = new VinculoUsuarioCaixaInstitucional(
                usuario.getId(),
                unidade,
                caixa,
                FuncaoOperacionalInstitucional.SERVIDOR_TRIAGEM,
                AbrangenciaVinculoInstitucional.UNIDADE,
                EnumSet.of(CapacidadeCaixaInstitucional.VISUALIZAR, CapacidadeCaixaInstitucional.RECEBER_COMUNICACAO),
                "nomeacao_homologada"
        );

        InstitutionalAffiliation affiliation = new InstitutionalAffiliation(
                "AFF-UNIDADE-PRISIONAL",
                DestinatarioInstitucionalKind.UNIDADE_PRISIONAL,
                "PPCE",
                "Polícia Penal do Ceará",
                "UP_001",
                "Unidade Prisional Fortaleza I",
                InstitutionalOrganizationScope.UNIDADE_PRISIONAL,
                "UNIDADE_PRISIONAL",
                "CE",
                "Fortaleza",
                null,
                "ESTADO",
                List.of("PENAL"),
                List.of("FORTALEZA"),
                "pp.ce.gov.br",
                "Diretor da unidade",
                usuario.getId(),
                InstitutionalNominationRole.DIRETOR_UNIDADE_PRISIONAL,
                "seguranca@pp.ce.gov.br",
                List.of("PJB_INBOX"),
                List.of("CIENCIA_PESSOAL"),
                List.of("48H"),
                List.of("EMAIL"),
                List.of("API"),
                InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
                true,
                true,
                true,
                true,
                InstitutionalAffiliationStatus.HOMOLOGADA,
                List.of("adesao_institucional_homologada"),
                Instant.now().minusSeconds(120),
                Instant.now().minusSeconds(60),
                null
        );

        InstitutionalNomination nomination = new InstitutionalNomination(
                "NOM-1",
                affiliation.affiliationId(),
                usuario.getId(),
                usuario.getNome(),
                TipoUsuario.SERVIDOR,
                InstitutionalAccessLaneKind.CUSTODIA,
                InstitutionalNominationRole.TRIAGEM_ORGAO,
                FuncaoOperacionalInstitucional.SERVIDOR_TRIAGEM,
                InstitutionalProcessProfile.OPERADOR_CUSTODIA_PRISIONAL,
                unidade.codigo(),
                caixa.codigo(),
                EnumSet.of(CapacidadeCaixaInstitucional.VISUALIZAR, CapacidadeCaixaInstitucional.RECEBER_COMUNICACAO),
                InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
                null,
                InstitutionalNominationStatus.ATIVA,
                Instant.now().minusSeconds(300),
                null,
                true,
                true,
                true,
                false,
                null,
                Instant.now().minusSeconds(300),
                Instant.now().minusSeconds(60)
        );

        when(currentUserService.getRequired()).thenReturn(usuario);
        when(inboxRepository.findAll()).thenReturn(List.of());
        when(delegationRepository.findAll()).thenReturn(List.of());
        when(coverageRepository.findAll()).thenReturn(List.of());
        when(nominationRepository.findByNominatedUserId(usuario.getId())).thenReturn(List.of(nomination));
        when(affiliationRepository.findAll()).thenReturn(List.of(affiliation));
        when(vinculoResolver.resolver(eq(usuario), any(DestinatarioInstitucionalKind.class), eq("CE"), eq("Fortaleza"))).thenReturn(List.of());
        when(vinculoResolver.resolver(eq(usuario), eq(DestinatarioInstitucionalKind.UNIDADE_PRISIONAL), eq("CE"), eq("Fortaleza"))).thenReturn(List.of(vinculo));

        var summary = service.resolverEntradaAtual();

        assertTrue(summary.possuiAmbienteInstitucional());
        assertFalse(summary.contextos().isEmpty());
        var contexto = summary.contextos().stream()
                .filter(item -> item.destinatarioKind() == DestinatarioInstitucionalKind.UNIDADE_PRISIONAL)
                .findFirst()
                .orElseThrow();
        assertEquals(InstitutionalEntryLandingPanel.PAINEL_CUSTODIA_PRISIONAL, contexto.landingPanel());
        assertTrue(contexto.landingPath().contains("scope=UNIDADE_PRISIONAL"));
        assertTrue(contexto.fundamentosEntrada().stream().anyMatch(item -> item.equals("nomeacao=TRIAGEM_ORGAO") || item.equals("blueprint_scope=UNIDADE_PRISIONAL")));
    }
}
