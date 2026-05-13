package com.tcc.pjb.backend.core.frontend.app.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeModeView;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.GovBrAssuranceExtractor;
import com.tcc.pjb.backend.core.security.GovBrAssurancePolicy;
import com.tcc.pjb.backend.model.dto.profile.CapabilityExtensionResponse;
import com.tcc.pjb.backend.model.dto.security.context.PjbAuthenticatedSessionResponse;
import com.tcc.pjb.backend.model.dto.security.context.SecurityContextResponse;
import com.tcc.pjb.backend.model.dto.security.context.SecurityHatResponse;
import com.tcc.pjb.backend.model.dto.security.context.SecurityStateResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeAffiliationInviteService;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeGovernedDocumentFilingService;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeGovernedExternalProtocolService;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeGovernedPetitionService;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeGovernedMultimediaWorkspaceService;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeGovernedUploadIngressService;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeProcessTransferService;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeProcessWorkspaceScopeService;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeSignatureQueueService;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeWorkspaceCreationService;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeWorkspaceDashboardService;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeWorkspaceModeService;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeWorkspaceLegalCockpitService;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeWorkspaceMainDashboardService;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeWorkspaceTeamAvatarService;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeWorkspaceExecutiveDashboardService;
import com.tcc.pjb.backend.service.professional.ProfessionalOrganExecutiveDashboardService;
import com.tcc.pjb.backend.service.professional.ProfessionalRoleExecutiveDashboardService;
import com.tcc.pjb.backend.service.professional.ProfessionalForensicExecutiveDashboardService;
import com.tcc.pjb.backend.service.profile.surface.PerfilCapabilitySurfaceFacadeService;
import com.tcc.pjb.backend.service.security.surface.SecurityContextSurfaceFacadeService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;

class PjbFrontendAppApplicationServiceTest {

    @Test
    void bootstrap_deveMaterializarSessaoCapacidadesMenuECatalogos() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        PerfilCapabilitySurfaceFacadeService capabilitySurfaceFacadeService = mock(PerfilCapabilitySurfaceFacadeService.class);
        SecurityContextSurfaceFacadeService securityContextSurfaceFacadeService = mock(SecurityContextSurfaceFacadeService.class);
        GovBrAssuranceExtractor assuranceExtractor = mock(GovBrAssuranceExtractor.class);
        GovBrAssurancePolicy assurancePolicy = mock(GovBrAssurancePolicy.class);
        OfficeWorkspaceModeService officeWorkspaceModeService = mock(OfficeWorkspaceModeService.class);
        OfficeWorkspaceCreationService officeWorkspaceCreationService = mock(OfficeWorkspaceCreationService.class);
        OfficeWorkspaceDashboardService officeWorkspaceDashboardService = mock(OfficeWorkspaceDashboardService.class);
        OfficeAffiliationInviteService officeAffiliationInviteService = mock(OfficeAffiliationInviteService.class);
        OfficeProcessTransferService officeProcessTransferService = mock(OfficeProcessTransferService.class);
        OfficeProcessWorkspaceScopeService officeProcessWorkspaceScopeService = mock(OfficeProcessWorkspaceScopeService.class);
        OfficeGovernedDocumentFilingService officeGovernedDocumentFilingService = mock(OfficeGovernedDocumentFilingService.class);
        OfficeGovernedExternalProtocolService officeGovernedExternalProtocolService = mock(OfficeGovernedExternalProtocolService.class);
        OfficeGovernedPetitionService officeGovernedPetitionService = mock(OfficeGovernedPetitionService.class);
        OfficeGovernedUploadIngressService officeGovernedUploadIngressService = mock(OfficeGovernedUploadIngressService.class);
        OfficeGovernedMultimediaWorkspaceService officeGovernedMultimediaWorkspaceService = mock(OfficeGovernedMultimediaWorkspaceService.class);
        OfficeSignatureQueueService officeSignatureQueueService = mock(OfficeSignatureQueueService.class);
        OfficeWorkspaceLegalCockpitService officeWorkspaceLegalCockpitService = mock(OfficeWorkspaceLegalCockpitService.class);
        OfficeWorkspaceMainDashboardService officeWorkspaceMainDashboardService = mock(OfficeWorkspaceMainDashboardService.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);

        Usuario usuario = new Usuario();
        usuario.setId(10L);
        usuario.setNome("Tiago Silva");
        usuario.setEmail("tiago@example.com");
        usuario.setCpf("12345678901");
        usuario.setTipoUsuario(TipoUsuario.CIDADAO);
        usuario.setPerfil("CIDADAO");
        usuario.setUf("CE");
        usuario.setComarca("Morada Nova");
        usuario.setAtivo(true);

        when(currentUserService.getRequired()).thenReturn(usuario);
        when(capabilitySurfaceFacadeService.capacidades(null)).thenReturn(new CapabilityExtensionResponse("CIDADAO", List.of("DASHBOARD_PROCESSOS_PROPRIOS", "CALENDARIO_AUDIENCIAS")));
        when(assuranceExtractor.extract(org.mockito.ArgumentMatchers.any())).thenReturn("prata");
        when(assurancePolicy.exigeStepUp("prata", true)).thenReturn(true);
        when(securityContextSurfaceFacadeService.context(org.mockito.ArgumentMatchers.any())).thenReturn(new SecurityContextResponse(
                10L,
                "tiago@example.com",
                "CIDADAO",
                List.of(new SecurityHatResponse(null, "Atuação Independente", "INDEPENDENTE")),
                new SecurityStateResponse(false, null, true, LocalDateTime.parse("2026-04-12T10:15:00"), true, true, List.of("VERIFY_TRUSTED_DEVICE"), null, List.of()),
                new PjbAuthenticatedSessionResponse(true, true, false, "JWT", "BEARER", "tiago@example.com", "sub-1", "issuer", "10", "12345678901", "tiago@example.com", "loa2", List.of("pwd"), List.of("ROLE_CIDADAO"), null, "prata", true, false, true, null, null, null, "/cidadao", "ACTIVE", "frontend-dev", "primary", true, true, true, true, false, List.of("jwt"), Instant.parse("2026-04-12T10:00:00Z"))));
        when(officeWorkspaceModeService.current(org.mockito.ArgumentMatchers.any())).thenReturn(new PjbFrontendOfficeModeView("PERSONAL", null, null, null, null, false, true, false, false, false, false, List.of(), List.of("Processos proprios em primeiro plano."), List.of("CIVIL", "PENAL"), true, null, null, null, false, 10L, "Tiago Silva"));

        PjbFrontendAppApplicationService service = new PjbFrontendAppApplicationService(
                currentUserService,
                capabilitySurfaceFacadeService,
                securityContextSurfaceFacadeService,
                assuranceExtractor,
                assurancePolicy,
                officeWorkspaceModeService,
                officeWorkspaceCreationService,
                officeWorkspaceDashboardService,
                officeAffiliationInviteService,
                officeProcessTransferService,
                officeProcessWorkspaceScopeService,
                officeGovernedDocumentFilingService,
                officeGovernedPetitionService,
                officeGovernedExternalProtocolService,
                officeGovernedUploadIngressService,
                officeGovernedMultimediaWorkspaceService,
                officeSignatureQueueService,
                officeWorkspaceLegalCockpitService,
                officeWorkspaceMainDashboardService,
                mock(OfficeWorkspaceExecutiveDashboardService.class),
                mock(ProfessionalForensicExecutiveDashboardService.class),
                mock(ProfessionalRoleExecutiveDashboardService.class),
                mock(ProfessionalOrganExecutiveDashboardService.class),
                mock(OfficeWorkspaceTeamAvatarService.class),
                auditLedgerService);

        TestingAuthenticationToken authentication = new TestingAuthenticationToken("tiago", "n/a", "ROLE_CIDADAO");
        MockHttpServletRequest request = new MockHttpServletRequest();

        var me = service.me(authentication);
        var capabilities = service.capabilities();
        var context = service.context(authentication, request);
        var menu = service.menu(authentication);
        var catalogs = service.supportCatalogs();
        var ramoCatalog = service.ramoDireitoCatalog();
        var bootstrap = service.bootstrap(authentication, request);

        assertThat(me.nome()).isEqualTo("Tiago Silva");
        assertThat(me.govBrAssuranceLevel()).isEqualTo("prata");
        assertThat(capabilities.capabilityCount()).isEqualTo(2);
        assertThat(context.authenticated()).isTrue();
        assertThat(context.pendingStepCount()).isEqualTo(1);
        assertThat(menu).extracting("code").contains("dashboard", "processos", "perfil");
        assertThat(catalogs.tipoUsuarios()).contains("CIDADAO", "ADVOGADO");
        assertThat(catalogs.ramosDireito()).contains("PROCESSUAL_CIVIL", "EXECUCAO_FISCAL");
        assertThat(ramoCatalog).extracting("name").contains("CIVIL", "PROCESSUAL_CIVIL", "EXECUCAO_FISCAL");
        assertThat(bootstrap.nextApiCalls()).contains("/api/v1/frontend/app/me", "/api/v1/cidadao/painel");
        assertThat(bootstrap.menu()).hasSizeGreaterThanOrEqualTo(3);
        assertThat(bootstrap.officeMode().mode()).isEqualTo("PERSONAL");
    }
}
