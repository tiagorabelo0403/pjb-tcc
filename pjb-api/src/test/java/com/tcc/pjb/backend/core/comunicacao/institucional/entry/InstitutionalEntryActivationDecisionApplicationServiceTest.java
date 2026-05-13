package com.tcc.pjb.backend.core.comunicacao.institucional.entry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.comunicacao.institucional.access.application.InstitutionalStepUpAuthenticationPolicyApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalStepUpAuthenticationPolicy;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalNomination;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalNominationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.InstitutionalApiRoutes;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.application.InstitutionalEntryActivationDecisionApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.application.InstitutionalEntryContextApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntryActivationBundle;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntryContext;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntryLandingPanel;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntrySummary;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalIdentityBaseProfile;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalProcessProfile;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalOperationalProfileProjectionApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalSessionRiskApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOperationalProfileProjection;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalRiskSeverity;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalSessionRiskAssessment;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalSessionRiskFinding;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.application.InstitutionalPanelProvisioningReadinessApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain.InstitutionalHearingSchedulingGovernance;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain.InstitutionalOperationalDeskGovernance;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain.InstitutionalPanelProvisioningReadiness;
import com.tcc.pjb.backend.core.identity.govbr.application.GovBrIdentityAssuranceApplicationService;
import com.tcc.pjb.backend.core.identity.govbr.domain.GovBrIdentityAssuranceAggregate;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.FuncaoOperacionalInstitucional;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalAccessLaneKind;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalEntryMode;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalNominationRole;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalNominationStatus;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalTrustLevel;
import com.tcc.pjb.backend.model.entity.enums.OrganizacaoExtraJudicialKind;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class InstitutionalEntryActivationDecisionApplicationServiceTest {

    @Test
    void mustActivateInstitutionalPanelWhenProfileIsReadyAndSecurityBindingIsStrong() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        InstitutionalEntryContextApplicationService entryContextApplicationService = mock(InstitutionalEntryContextApplicationService.class);
        InstitutionalNominationStateRepository nominationRepository = mock(InstitutionalNominationStateRepository.class);
        InstitutionalOperationalProfileProjectionApplicationService profileService = mock(InstitutionalOperationalProfileProjectionApplicationService.class);
        InstitutionalSessionRiskApplicationService sessionRiskService = mock(InstitutionalSessionRiskApplicationService.class);
        InstitutionalStepUpAuthenticationPolicyApplicationService stepUpService = mock(InstitutionalStepUpAuthenticationPolicyApplicationService.class);
        GovBrIdentityAssuranceApplicationService govBrIdentityAssuranceApplicationService = mock(GovBrIdentityAssuranceApplicationService.class);
        InstitutionalPanelProvisioningReadinessApplicationService panelProvisioningReadinessApplicationService = mock(InstitutionalPanelProvisioningReadinessApplicationService.class);
        InstitutionalEntryActivationDecisionApplicationService service = new InstitutionalEntryActivationDecisionApplicationService(
                currentUserService,
                entryContextApplicationService,
                nominationRepository,
                profileService,
                sessionRiskService,
                stepUpService,
                govBrIdentityAssuranceApplicationService,
                panelProvisioningReadinessApplicationService);
        Instant now = Instant.now();
        Usuario usuario = usuario(101L, "Maria Gestora", TipoUsuario.SERVIDOR_FORUM);
        InstitutionalEntrySummary summary = summary(101L, "Maria Gestora");
        InstitutionalNomination nomination = nomination(101L, "Maria Gestora", now);
        InstitutionalOperationalProfileProjection profile = profile(101L, "Maria Gestora", now);
        InstitutionalSessionRiskAssessment riskAssessment = new InstitutionalSessionRiskAssessment(
                "RSK-1",
                101L,
                "Maria Gestora",
                "AFF-1",
                "NOM-1",
                "UNI-1",
                "CX-1",
                "device-1",
                "10.0.0.2",
                "CE",
                5,
                "BAIXO",
                false,
                false,
                false,
                List.of(),
                List.of("sessao_estavel"),
                now,
                null);
        InstitutionalStepUpAuthenticationPolicy policy = new InstitutionalStepUpAuthenticationPolicy(
                101L,
                "Maria Gestora",
                "AFF-1",
                "NOM-1",
                "ASSINAR_MANIFESTACAO",
                false,
                false,
                false,
                true,
                false,
                false,
                List.of(),
                List.of("policy_ok"),
                now);
        GovBrIdentityAssuranceAggregate gov = new GovBrIdentityAssuranceAggregate(
                true,
                101L,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                "ALTO",
                List.of(),
                List.of(),
                List.of("TRUSTED_DEVICE_ATIVO"),
                now);

        when(currentUserService.getRequired()).thenReturn(usuario);
        when(nominationRepository.findByNominatedUserId(101L)).thenReturn(List.of(nomination));
        when(profileService.materializar("AFF-1", "NOM-1")).thenReturn(profile);
        when(panelProvisioningReadinessApplicationService.avaliar(profile)).thenReturn(panelProvisioning(true, now));
        when(panelProvisioningReadinessApplicationService.avaliar(profile)).thenReturn(panelProvisioning(true, now));
        when(sessionRiskService.avaliarAtual("AFF-1", "NOM-1", "UNI-1", "CX-1")).thenReturn(riskAssessment);
        when(stepUpService.avaliarAtual("AFF-1", "NOM-1", "ASSINAR_MANIFESTACAO")).thenReturn(policy);
        when(govBrIdentityAssuranceApplicationService.atual()).thenReturn(gov);

        InstitutionalEntryActivationBundle bundle = service.avaliarEntradaAtual(summary);

        assertNotNull(bundle.operationalProfile());
        assertTrue(bundle.decision().activateInstitutionalContext());
        assertEquals("PAINEL_INSTITUCIONAL", bundle.decision().targetEnvironment());
        assertEquals(InstitutionalEntryMode.INSTITUCIONAL_AFILIADO.name(), bundle.decision().entryMode());
        assertEquals("/app/institucional/secretaria/uni-1/cx-1", bundle.decision().landingPath());
        assertFalse(bundle.decision().requiresStepUp());
        assertTrue(bundle.decision().panelProvisioningComplete());
        assertFalse(bundle.decision().requiresPanelProvisioningReview());
        assertTrue(bundle.decision().garantias().contains("ATIVACAO_DIRETA_DO_PAINEL_INSTITUCIONAL_LIBERADA"));
    }

    @Test
    void mustHoldInstitutionalEntryForStepUpWhenRiskAndBindingAreNotEnough() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        InstitutionalEntryContextApplicationService entryContextApplicationService = mock(InstitutionalEntryContextApplicationService.class);
        InstitutionalNominationStateRepository nominationRepository = mock(InstitutionalNominationStateRepository.class);
        InstitutionalOperationalProfileProjectionApplicationService profileService = mock(InstitutionalOperationalProfileProjectionApplicationService.class);
        InstitutionalSessionRiskApplicationService sessionRiskService = mock(InstitutionalSessionRiskApplicationService.class);
        InstitutionalStepUpAuthenticationPolicyApplicationService stepUpService = mock(InstitutionalStepUpAuthenticationPolicyApplicationService.class);
        GovBrIdentityAssuranceApplicationService govBrIdentityAssuranceApplicationService = mock(GovBrIdentityAssuranceApplicationService.class);
        InstitutionalPanelProvisioningReadinessApplicationService panelProvisioningReadinessApplicationService = mock(InstitutionalPanelProvisioningReadinessApplicationService.class);
        InstitutionalEntryActivationDecisionApplicationService service = new InstitutionalEntryActivationDecisionApplicationService(
                currentUserService,
                entryContextApplicationService,
                nominationRepository,
                profileService,
                sessionRiskService,
                stepUpService,
                govBrIdentityAssuranceApplicationService,
                panelProvisioningReadinessApplicationService);
        Instant now = Instant.now();
        Usuario usuario = usuario(202L, "Joao Secretaria", TipoUsuario.SERVIDOR_FORUM);
        InstitutionalEntrySummary summary = summary(202L, "Joao Secretaria");
        InstitutionalNomination nomination = nomination(202L, "Joao Secretaria", now);
        InstitutionalOperationalProfileProjection profile = profile(202L, "Joao Secretaria", now);
        InstitutionalSessionRiskAssessment riskAssessment = new InstitutionalSessionRiskAssessment(
                "RSK-2",
                202L,
                "Joao Secretaria",
                "AFF-1",
                "NOM-1",
                "UNI-1",
                "CX-1",
                "device-new",
                "177.0.0.1",
                "CE",
                78,
                "ALTO",
                true,
                false,
                false,
                List.of(new InstitutionalSessionRiskFinding("NOVO_DISPOSITIVO", InstitutionalRiskSeverity.MEDIA, false, "Novo dispositivo", List.of())),
                List.of("risco_exige_step_up"),
                now,
                null);
        InstitutionalStepUpAuthenticationPolicy policy = new InstitutionalStepUpAuthenticationPolicy(
                202L,
                "Joao Secretaria",
                "AFF-1",
                "NOM-1",
                "ASSINAR_MANIFESTACAO",
                true,
                true,
                true,
                true,
                false,
                false,
                List.of("step_up_govbr_obrigatorio"),
                List.of("policy_stepup"),
                now);
        GovBrIdentityAssuranceAggregate gov = new GovBrIdentityAssuranceAggregate(
                true,
                202L,
                false,
                false,
                true,
                true,
                true,
                false,
                false,
                "BASICO",
                List.of("VINCULO_GOVBR_AUSENTE"),
                List.of("DISPOSITIVO_CONFIAVEL_AINDA_NAO_VALIDADO"),
                List.of(),
                now);

        when(currentUserService.getRequired()).thenReturn(usuario);
        when(nominationRepository.findByNominatedUserId(202L)).thenReturn(List.of(nomination));
        when(profileService.materializar("AFF-1", "NOM-1")).thenReturn(profile);
        when(panelProvisioningReadinessApplicationService.avaliar(profile)).thenReturn(panelProvisioning(true, now));
        when(panelProvisioningReadinessApplicationService.avaliar(profile)).thenReturn(panelProvisioning(true, now));
        when(sessionRiskService.avaliarAtual("AFF-1", "NOM-1", "UNI-1", "CX-1")).thenReturn(riskAssessment);
        when(stepUpService.avaliarAtual("AFF-1", "NOM-1", "ASSINAR_MANIFESTACAO")).thenReturn(policy);
        when(govBrIdentityAssuranceApplicationService.atual()).thenReturn(gov);

        InstitutionalEntryActivationBundle bundle = service.avaliarEntradaAtual(summary);

        assertFalse(bundle.decision().activateInstitutionalContext());
        assertEquals("AGUARDANDO_VINCULO_GOVBR", bundle.decision().targetEnvironment());
        assertTrue(bundle.decision().routeToPersonalPanel());
        assertTrue(bundle.decision().requiresGovBrBinding());
        assertTrue(bundle.decision().requiresTrustedDevice());
        assertTrue(bundle.decision().requiresStepUp());
        assertEquals("/api/v1/auth/govbr/stepup/start", bundle.decision().stepUpStartPath());
        assertTrue(bundle.decision().panelProvisioningComplete());
        assertTrue(bundle.decision().blockers().contains("VINCULO_GOVBR_AUSENTE"));
        assertTrue(bundle.decision().warnings().contains("NOVO_DISPOSITIVO"));
    }


    @Test
    void mustHonorExplicitNominationWhenActivationIsRequestedForCanonicalInstitutionalRoute() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        InstitutionalEntryContextApplicationService entryContextApplicationService = mock(InstitutionalEntryContextApplicationService.class);
        InstitutionalNominationStateRepository nominationRepository = mock(InstitutionalNominationStateRepository.class);
        InstitutionalOperationalProfileProjectionApplicationService profileService = mock(InstitutionalOperationalProfileProjectionApplicationService.class);
        InstitutionalSessionRiskApplicationService sessionRiskService = mock(InstitutionalSessionRiskApplicationService.class);
        InstitutionalStepUpAuthenticationPolicyApplicationService stepUpService = mock(InstitutionalStepUpAuthenticationPolicyApplicationService.class);
        GovBrIdentityAssuranceApplicationService govBrIdentityAssuranceApplicationService = mock(GovBrIdentityAssuranceApplicationService.class);
        InstitutionalPanelProvisioningReadinessApplicationService panelProvisioningReadinessApplicationService = mock(InstitutionalPanelProvisioningReadinessApplicationService.class);
        InstitutionalEntryActivationDecisionApplicationService service = new InstitutionalEntryActivationDecisionApplicationService(
                currentUserService,
                entryContextApplicationService,
                nominationRepository,
                profileService,
                sessionRiskService,
                stepUpService,
                govBrIdentityAssuranceApplicationService,
                panelProvisioningReadinessApplicationService);
        Instant now = Instant.now();
        Usuario usuario = usuario(303L, "Ana Vinculada", TipoUsuario.SERVIDOR_FORUM);
        InstitutionalEntrySummary summary = summary(303L, "Ana Vinculada");
        InstitutionalNomination nomination = nomination(303L, "Ana Vinculada", now);
        InstitutionalOperationalProfileProjection profile = profile(303L, "Ana Vinculada", now);
        InstitutionalSessionRiskAssessment riskAssessment = new InstitutionalSessionRiskAssessment(
                "RSK-3",
                303L,
                "Ana Vinculada",
                "AFF-1",
                "NOM-1",
                "UNI-1",
                "CX-1",
                "device-3",
                "10.0.0.9",
                "CE",
                8,
                "BAIXO",
                false,
                false,
                false,
                List.of(),
                List.of("sessao_estavel"),
                now,
                null);
        InstitutionalStepUpAuthenticationPolicy policy = new InstitutionalStepUpAuthenticationPolicy(
                303L,
                "Ana Vinculada",
                "AFF-1",
                "NOM-1",
                "ASSINAR_MANIFESTACAO",
                false,
                false,
                false,
                true,
                false,
                false,
                List.of(),
                List.of("policy_ok"),
                now);
        GovBrIdentityAssuranceAggregate gov = new GovBrIdentityAssuranceAggregate(
                true,
                303L,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                "ALTO",
                List.of(),
                List.of(),
                List.of("TRUSTED_DEVICE_ATIVO"),
                now);

        when(currentUserService.getRequired()).thenReturn(usuario);
        when(entryContextApplicationService.resolverEntradaAtual()).thenReturn(summary);
        when(nominationRepository.findByNominationId("NOM-1")).thenReturn(java.util.Optional.of(nomination));
        when(profileService.materializar("AFF-1", "NOM-1")).thenReturn(profile);
        when(panelProvisioningReadinessApplicationService.avaliar(profile)).thenReturn(panelProvisioning(true, now));
        when(sessionRiskService.avaliarAtual("AFF-1", "NOM-1", "UNI-1", "CX-1")).thenReturn(riskAssessment);
        when(stepUpService.avaliarAtual("AFF-1", "NOM-1", "ASSINAR_MANIFESTACAO")).thenReturn(policy);
        when(govBrIdentityAssuranceApplicationService.atual()).thenReturn(gov);

        InstitutionalEntryActivationBundle bundle = service.avaliarEntradaAtual("AFF-1", "NOM-1");

        assertEquals("NOM-1", bundle.decision().nominationId());
        assertEquals("AFF-1", bundle.decision().affiliationId());
        assertTrue(bundle.decision().activateInstitutionalContext());
        verify(nominationRepository).findByNominationId("NOM-1");
    }

    @Test
    void mustRouteToPersonalPanelWhenInstitutionalPanelProvisioningIsIncomplete() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        InstitutionalEntryContextApplicationService entryContextApplicationService = mock(InstitutionalEntryContextApplicationService.class);
        InstitutionalNominationStateRepository nominationRepository = mock(InstitutionalNominationStateRepository.class);
        InstitutionalOperationalProfileProjectionApplicationService profileService = mock(InstitutionalOperationalProfileProjectionApplicationService.class);
        InstitutionalSessionRiskApplicationService sessionRiskService = mock(InstitutionalSessionRiskApplicationService.class);
        InstitutionalStepUpAuthenticationPolicyApplicationService stepUpService = mock(InstitutionalStepUpAuthenticationPolicyApplicationService.class);
        GovBrIdentityAssuranceApplicationService govBrIdentityAssuranceApplicationService = mock(GovBrIdentityAssuranceApplicationService.class);
        InstitutionalPanelProvisioningReadinessApplicationService panelProvisioningReadinessApplicationService = mock(InstitutionalPanelProvisioningReadinessApplicationService.class);
        InstitutionalEntryActivationDecisionApplicationService service = new InstitutionalEntryActivationDecisionApplicationService(
                currentUserService,
                entryContextApplicationService,
                nominationRepository,
                profileService,
                sessionRiskService,
                stepUpService,
                govBrIdentityAssuranceApplicationService,
                panelProvisioningReadinessApplicationService);
        Instant now = Instant.now();
        Usuario usuario = usuario(404L, "Beatriz Painel", TipoUsuario.SERVIDOR_FORUM);
        InstitutionalEntrySummary summary = summary(404L, "Beatriz Painel");
        InstitutionalNomination nomination = nomination(404L, "Beatriz Painel", now);
        InstitutionalOperationalProfileProjection profile = profile(404L, "Beatriz Painel", now);
        InstitutionalSessionRiskAssessment riskAssessment = new InstitutionalSessionRiskAssessment(
                "RSK-4",
                404L,
                "Beatriz Painel",
                "AFF-1",
                "NOM-1",
                "UNI-1",
                "CX-1",
                "device-4",
                "10.0.0.4",
                "CE",
                6,
                "BAIXO",
                false,
                false,
                false,
                List.of(),
                List.of("sessao_estavel"),
                now,
                null);
        GovBrIdentityAssuranceAggregate gov = new GovBrIdentityAssuranceAggregate(
                true,
                404L,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                "ALTO",
                List.of(),
                List.of(),
                List.of("TRUSTED_DEVICE_ATIVO"),
                now);

        when(currentUserService.getRequired()).thenReturn(usuario);
        when(nominationRepository.findByNominatedUserId(404L)).thenReturn(List.of(nomination));
        when(profileService.materializar("AFF-1", "NOM-1")).thenReturn(profile);
        when(panelProvisioningReadinessApplicationService.avaliar(profile)).thenReturn(panelProvisioning(false, now));
        when(sessionRiskService.avaliarAtual("AFF-1", "NOM-1", "UNI-1", "CX-1")).thenReturn(riskAssessment);
        when(govBrIdentityAssuranceApplicationService.atual()).thenReturn(gov);

        InstitutionalEntryActivationBundle bundle = service.avaliarEntradaAtual(summary);

        assertFalse(bundle.decision().activateInstitutionalContext());
        assertTrue(bundle.decision().requiresPanelProvisioningReview());
        assertFalse(bundle.decision().panelProvisioningComplete());
        assertEquals("AGUARDANDO_PROVISIONAMENTO_PAINEL", bundle.decision().targetEnvironment());
        assertTrue(bundle.decision().routeToPersonalPanel());
        assertTrue(bundle.decision().panelProvisioningFindings().contains("painel_sem_superficie_datas_audiencia"));
    }

    private Usuario usuario(Long id, String nome, TipoUsuario tipoUsuario) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNome(nome);
        usuario.setTipoUsuario(tipoUsuario);
        usuario.setUf("CE");
        usuario.setComarca("Morada Nova");
        return usuario;
    }

    private InstitutionalEntrySummary summary(Long userId, String userName) {
        InstitutionalEntryContext context = new InstitutionalEntryContext(
                "CTX-1",
                DestinatarioInstitucionalKind.MINISTERIO_PUBLICO,
                OrganizacaoExtraJudicialKind.MINISTERIO_PUBLICO,
                "MPCE",
                "Ministerio Publico do Ceara",
                "UNI-1",
                "Promotoria de Morada Nova",
                "NUCLEO-1",
                "CE",
                "Morada Nova",
                "CX-1",
                "Caixa Principal",
                InstitutionalProcessProfile.SECRETARIA_FORUM,
                FuncaoOperacionalInstitucional.GESTOR_CAIXA,
                EnumSet.of(CapacidadeCaixaInstitucional.ASSINAR_MANIFESTACAO, CapacidadeCaixaInstitucional.VISUALIZAR),
                false,
                false,
                false,
                false,
                12,
                3,
                2,
                1,
                InstitutionalEntryLandingPanel.PAINEL_SECRETARIA_FORUM,
                "/app/institucional/secretaria/uni-1/cx-1",
                "amber",
                100,
                List.of("contexto_preferencial_materializado"));
        InstitutionalIdentityBaseProfile identity = new InstitutionalIdentityBaseProfile(
                "ID-1",
                TipoUsuario.SERVIDOR_FORUM,
                true,
                InstitutionalEntryMode.INSTITUCIONAL_AFILIADO,
                InstitutionalProcessProfile.SECRETARIA_FORUM,
                InstitutionalEntryLandingPanel.PAINEL_SECRETARIA_FORUM,
                InstitutionalTrustLevel.NIVEL_2_NOMEACAO_ATIVA,
                true,
                List.of("identidade_institucional_base"));
        return new InstitutionalEntrySummary(
                userId,
                userName,
                TipoUsuario.SERVIDOR_FORUM,
                identity,
                true,
                true,
                List.of(context),
                context,
                Instant.now());
    }

    private InstitutionalNomination nomination(Long userId, String userName, Instant now) {
        return new InstitutionalNomination(
                "NOM-1",
                "AFF-1",
                userId,
                userName,
                TipoUsuario.SERVIDOR_FORUM,
                InstitutionalAccessLaneKind.SECRETARIA,
                InstitutionalNominationRole.SECRETARIA_FORUM,
                FuncaoOperacionalInstitucional.GESTOR_CAIXA,
                InstitutionalProcessProfile.SECRETARIA_FORUM,
                "UNI-1",
                "CX-1",
                EnumSet.of(CapacidadeCaixaInstitucional.ASSINAR_MANIFESTACAO, CapacidadeCaixaInstitucional.VISUALIZAR),
                InstitutionalTrustLevel.NIVEL_2_NOMEACAO_ATIVA,
                InstitutionalEntryLandingPanel.PAINEL_SECRETARIA_FORUM,
                InstitutionalNominationStatus.ATIVA,
                now.minusSeconds(120),
                now.plusSeconds(3600),
                true,
                true,
                true,
                true,
                null,
                now,
                now);
    }

    private InstitutionalOperationalProfileProjection profile(Long userId, String userName, Instant now) {
        return new InstitutionalOperationalProfileProjection(
                "PROFILE-1",
                "ATIVO_NO_PJB",
                true,
                "AFF-1",
                "NOM-1",
                userId,
                userName,
                "SERVIDOR_FORUM",
                "PROMOTORIA",
                "MINISTERIO_PUBLICO",
                "MPCE",
                "Ministerio Publico do Ceara",
                "UNI-1",
                "Promotoria de Morada Nova",
                "CX-1",
                "SECRETARIA",
                "SECRETARIA_FORUM",
                "GESTOR_CAIXA",
                "SECRETARIA_FORUM",
                "PAINEL_SECRETARIA_FORUM",
                "/app/institucional/secretaria/uni-1/cx-1",
                "amber",
                "SECRETARIA_FORUM",
                "NIVEL_2_MFA_FORTE",
                true,
                true,
                true,
                false,
                true,
                true,
                "LOCAL",
                "MPCE",
                "UNI-1",
                "Promotoria de Morada Nova",
                "Morada Nova",
                "CE|MPCE|UNI-1|CX-1|B01",
                "CE|MPCE|UNI-1|CX-1",
                "RR-NE-01",
                List.of("ASSINAR_MANIFESTACAO", "VISUALIZAR"),
                List.of("PJB", "DIRETOR_GERAL"),
                List.of("PJB", "DIRETOR_GERAL"),
                List.of(),
                List.of(),
                List.of("perfil_materializado_e_visivel_no_pjb"),
                now);
    }

    private InstitutionalPanelProvisioningReadiness panelProvisioning(boolean complete, Instant now) {
        return new InstitutionalPanelProvisioningReadiness(
                "PROMOTORIA__PROMOTOR_TITULAR",
                "PAINEL_SECRETARIA_FORUM",
                InstitutionalApiRoutes.painelExecutivoComUnidade("UNI-1"),
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                complete,
                complete,
                true,
                complete,
                true,
                true,
                true,
                true,
                complete,
                complete,
                1,
                4,
                4,
                2,
                2,
                4,
                8,
                4,
                3,
                7,
                complete ? 7 : 4,
                List.of("fila_geral_de_triagem"),
                List.of("dar_ciencia"),
                List.of("MFA obrigatório"),
                List.of("Gestão administrativa ampla"),
                List.of("calendario", "leitura", "triagem", "notificacoes"),
                complete ? List.of("NOTIFICACOES", "CALENDARIO_UNIFICADO", "DATAS_AUDIENCIA", "MODO_LEITURA", "TRIAGEM", "APRESENTACAO", "CORES") : List.of("NOTIFICACOES", "CALENDARIO_UNIFICADO", "TRIAGEM", "APRESENTACAO"),
                complete ? List.of() : List.of("DATAS_AUDIENCIA", "MODO_LEITURA", "CORES"),
                complete ? List.of() : List.of("painel_sem_superficie_datas_audiencia", "painel_sem_superficie_modo_leitura", "painel_sem_superficie_cores"),
                List.of("painel=PAINEL_SECRETARIA_FORUM"),
                new InstitutionalHearingSchedulingGovernance(
                        true,
                        true,
                        true,
                        true,
                        complete,
                        complete,
                        complete,
                        complete,
                        true,
                        true,
                        true,
                        complete,
                        true,
                        true,
                        complete,
                        true,
                        "CE|MPCE|UNI-1|CX-1|AUDIENCIAS",
                        List.of("fila_geral_de_triagem", "fila_preparo_audiencia"),
                        List.of("isolamento_por_unidade=UNI-1", "isolamento_por_scope=CE|MPCE|UNI-1|CX-1|AUDIENCIAS"),
                        List.of("MAGISTRADO_RESPONSAVEL", "SECRETARIA_FORUM"),
                        List.of("CIVEL_COMUM", "PENAL"),
                        List.of(),
                        complete ? List.of() : List.of("designacao_jurisdicional_autonoma_sem_chancela_do_magistrado"),
                        complete ? List.of() : List.of("painel_sem_capacidade_operacional_de_pauta"),
                        List.of("governanca_audiencia=ativa")),
                new InstitutionalOperationalDeskGovernance(
                        true,
                        true,
                        true,
                        true,
                        true,
                        true,
                        true,
                        true,
                        true,
                        true,
                        true,
                        true,
                        true,
                        true,
                        true,
                        true,
                        true,
                        true,
                        true,
                        "CE|PROMOTORIA|SECRETARIA_FORUM",
                        "TJCE|MORADA_NOVA|CIVEL",
                        "TJCE|MORADA_NOVA|UNI_1|VARA_1|CIVEL|CX_1",
                        "VARA_E_ESPECIALIZACAO",
                        "ESTADUAL_CIVEL",
                        "PROMOTORIA",
                        "TJCE|MORADA_NOVA|UNI_1|VARA_1|CIVEL|CX_1|PROMOTORIA|ESTADUAL_CIVEL",
                        List.of("tribunal=TJCE", "comarca=MORADA_NOVA", "unidade=UNI_1", "vara_cluster=VARA_1", "especializacao=CIVEL"),
                        List.of("EXPEDIENTE_SECRETARIA", "ORGANIZACAO_PROCESSUAL", "COMUNICACOES_PROCESSUAIS", "PROTOCOLO_DISTRIBUICAO_AUTUACAO"),
                        List.of("fila_entrada_unidade=TJCE_MORADA_NOVA_UNI_1_VARA_1_CIVEL_CX_1", "fila_secretaria=PROMOTORIA"),
                        List.of("tribunal=TJCE", "comarca=MORADA_NOVA", "unidade=UNI_1", "caixa=CX_1"),
                        List.of("PARTES_E_REPRESENTANTES_PROCESSUAIS", "MINISTERIO_PUBLICO"),
                        List.of("receber_expediente_de_secretaria", "organizar_processos_por_unidade_e_vara"),
                        List.of("preparar_minuta_sem_substituir_assinatura_final"),
                        List.of("corrigir_vinculo_de_unidade_em_situacao_excepcional"),
                        List.of("homologar_lotacao_ou_cobertura"),
                        List.of("registrar_prevencao_dependencia_redistribuicao_formal"),
                        List.of("expedir_mandado_oficio_carta_edital_ou_alvara_no_fluxo_competente"),
                        List.of("preparar_conclusao_voto_minuta_ou_sentenca_em_trilha_reservada"),
                        List.of("civel_contestacao_saneamento_instrucao_julgamento_e_cumprimento"),
                        List.of("mistura_operacional_entre_varas_ou_especializacoes"),
                        complete ? List.of() : List.of("mesa_operacional_sem_amarra_tribunal_comarca_unidade"),
                        List.of("governanca_mesa_operacional=ativa")),
                now);
    }

}
