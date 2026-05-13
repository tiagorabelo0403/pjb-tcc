package com.tcc.pjb.backend.core.frontend.app.application;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendAppBootstrapView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendCapabilitySummaryView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendContextView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendCurrentUserView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendMenuItemView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeAffiliationDecisionResultView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeModeView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeWorkspaceSummaryView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeProcessAccessView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeWorkspaceProcessPageView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeProcessTransferPreviewView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeProcessTransferView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeGovernedDocumentBatchLinkView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeGovernedPetitionView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeGovernedDocumentBatchPreviewView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeGovernedProtocolSubmissionView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeGovernedMultimediaWorkspaceView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeGovernedUploadBatchView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeGovernedUploadFinalizeView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeGovernedUploadIngressView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeGovernedUploadItemReservationView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeQueueItemView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeQueuePageView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOwnedOfficeView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeProcessReadingModeView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeWorkspaceLegalCockpitView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeWorkspaceMainDashboardView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeWorkspaceExecutiveDashboardView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendProfessionalWorkspaceExecutiveDashboardView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendProfessionalRoleExecutiveDashboardView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendProfessionalOrganExecutiveDashboardView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendRamoDireitoCatalogEntry;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendSupportCatalogView;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.GovBrAssuranceExtractor;
import com.tcc.pjb.backend.core.storage.ObjectReadResult;
import com.tcc.pjb.backend.core.security.GovBrAssurancePolicy;
import com.tcc.pjb.backend.model.dto.profile.CapabilityExtensionResponse;
import com.tcc.pjb.backend.model.dto.security.context.PjbAuthenticatedSessionResponse;
import com.tcc.pjb.backend.model.dto.security.context.SecurityContextResponse;
import com.tcc.pjb.backend.model.dto.security.context.SecurityHatResponse;
import com.tcc.pjb.backend.model.dto.security.context.SecurityStateResponse;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeAffiliationDecisionRequest;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeAffiliationFinalApprovalRequest;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeAffiliationInviteRequest;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeGovernedDocumentBatchLinkRequest;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeGovernedPetitionRequest;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeGovernedMultimediaWorkspaceRequest;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeGovernedProtocolSubmitRequest;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeGovernedUploadBatchCreateRequest;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeGovernedUploadFinalizeRequest;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeGovernedUploadReserveItemRequest;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeModeUpdateRequest;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeProcessTransferDecisionRequest;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeQueueDecisionRequest;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeWorkspaceProcessQueryRequest;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeProcessTransferRequest;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeWorkspaceCreateRequest;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.modules.advocacia.office.dto.OfficeQueueItemDto;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeQueueStatus;
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
import com.tcc.pjb.backend.service.professional.ProfessionalForensicExecutiveDashboardService;
import com.tcc.pjb.backend.service.professional.ProfessionalRoleExecutiveDashboardService;
import com.tcc.pjb.backend.service.professional.ProfessionalOrganExecutiveDashboardService;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeWorkspaceExecutiveDashboardService;
import com.tcc.pjb.backend.service.profile.surface.PerfilCapabilitySurfaceFacadeService;
import com.tcc.pjb.backend.service.security.surface.SecurityContextSurfaceFacadeService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PjbFrontendAppApplicationService {

    private final CurrentUserService currentUserService;
    private final PerfilCapabilitySurfaceFacadeService capabilitySurfaceFacadeService;
    private final SecurityContextSurfaceFacadeService securityContextSurfaceFacadeService;
    private final GovBrAssuranceExtractor govBrAssuranceExtractor;
    private final GovBrAssurancePolicy govBrAssurancePolicy;
    private final OfficeWorkspaceModeService officeWorkspaceModeService;
    private final OfficeWorkspaceCreationService officeWorkspaceCreationService;
    private final OfficeWorkspaceDashboardService officeWorkspaceDashboardService;
    private final OfficeAffiliationInviteService officeAffiliationInviteService;
    private final OfficeProcessTransferService officeProcessTransferService;
    private final OfficeProcessWorkspaceScopeService officeProcessWorkspaceScopeService;
    private final OfficeGovernedDocumentFilingService officeGovernedDocumentFilingService;
    private final OfficeGovernedPetitionService officeGovernedPetitionService;
    private final OfficeGovernedExternalProtocolService officeGovernedExternalProtocolService;
    private final OfficeGovernedUploadIngressService officeGovernedUploadIngressService;
    private final OfficeGovernedMultimediaWorkspaceService officeGovernedMultimediaWorkspaceService;
    private final OfficeSignatureQueueService officeSignatureQueueService;
    private final OfficeWorkspaceLegalCockpitService officeWorkspaceLegalCockpitService;
    private final OfficeWorkspaceMainDashboardService officeWorkspaceMainDashboardService;
    private final OfficeWorkspaceExecutiveDashboardService officeWorkspaceExecutiveDashboardService;
    private final ProfessionalForensicExecutiveDashboardService professionalForensicExecutiveDashboardService;
    private final ProfessionalRoleExecutiveDashboardService professionalRoleExecutiveDashboardService;
    private final ProfessionalOrganExecutiveDashboardService professionalOrganExecutiveDashboardService;
    private final OfficeWorkspaceTeamAvatarService officeWorkspaceTeamAvatarService;
    private final AuditLedgerService auditLedgerService;

    public PjbFrontendAppApplicationService(CurrentUserService currentUserService,
                                            PerfilCapabilitySurfaceFacadeService capabilitySurfaceFacadeService,
                                            SecurityContextSurfaceFacadeService securityContextSurfaceFacadeService,
                                            GovBrAssuranceExtractor govBrAssuranceExtractor,
                                            GovBrAssurancePolicy govBrAssurancePolicy,
                                            OfficeWorkspaceModeService officeWorkspaceModeService,
                                            OfficeWorkspaceCreationService officeWorkspaceCreationService,
                                            OfficeWorkspaceDashboardService officeWorkspaceDashboardService,
                                            OfficeAffiliationInviteService officeAffiliationInviteService,
                                            OfficeProcessTransferService officeProcessTransferService,
                                            OfficeProcessWorkspaceScopeService officeProcessWorkspaceScopeService,
                                            OfficeGovernedDocumentFilingService officeGovernedDocumentFilingService,
                                            OfficeGovernedPetitionService officeGovernedPetitionService,
                                            OfficeGovernedExternalProtocolService officeGovernedExternalProtocolService,
                                            OfficeGovernedUploadIngressService officeGovernedUploadIngressService,
                                            OfficeGovernedMultimediaWorkspaceService officeGovernedMultimediaWorkspaceService,
                                            OfficeSignatureQueueService officeSignatureQueueService,
                                            OfficeWorkspaceLegalCockpitService officeWorkspaceLegalCockpitService,
                                            OfficeWorkspaceMainDashboardService officeWorkspaceMainDashboardService,
                                            OfficeWorkspaceExecutiveDashboardService officeWorkspaceExecutiveDashboardService,
                                            ProfessionalForensicExecutiveDashboardService professionalForensicExecutiveDashboardService,
                                            ProfessionalRoleExecutiveDashboardService professionalRoleExecutiveDashboardService,
                                            ProfessionalOrganExecutiveDashboardService professionalOrganExecutiveDashboardService,
                                            OfficeWorkspaceTeamAvatarService officeWorkspaceTeamAvatarService,
                                            AuditLedgerService auditLedgerService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.capabilitySurfaceFacadeService = Objects.requireNonNull(capabilitySurfaceFacadeService);
        this.securityContextSurfaceFacadeService = Objects.requireNonNull(securityContextSurfaceFacadeService);
        this.govBrAssuranceExtractor = Objects.requireNonNull(govBrAssuranceExtractor);
        this.govBrAssurancePolicy = Objects.requireNonNull(govBrAssurancePolicy);
        this.officeWorkspaceModeService = Objects.requireNonNull(officeWorkspaceModeService);
        this.officeWorkspaceCreationService = Objects.requireNonNull(officeWorkspaceCreationService);
        this.officeWorkspaceDashboardService = Objects.requireNonNull(officeWorkspaceDashboardService);
        this.officeAffiliationInviteService = Objects.requireNonNull(officeAffiliationInviteService);
        this.officeProcessTransferService = Objects.requireNonNull(officeProcessTransferService);
        this.officeProcessWorkspaceScopeService = Objects.requireNonNull(officeProcessWorkspaceScopeService);
        this.officeGovernedDocumentFilingService = Objects.requireNonNull(officeGovernedDocumentFilingService);
        this.officeGovernedPetitionService = Objects.requireNonNull(officeGovernedPetitionService);
        this.officeGovernedExternalProtocolService = Objects.requireNonNull(officeGovernedExternalProtocolService);
        this.officeGovernedUploadIngressService = Objects.requireNonNull(officeGovernedUploadIngressService);
        this.officeGovernedMultimediaWorkspaceService = Objects.requireNonNull(officeGovernedMultimediaWorkspaceService);
        this.officeSignatureQueueService = Objects.requireNonNull(officeSignatureQueueService);
        this.officeWorkspaceLegalCockpitService = Objects.requireNonNull(officeWorkspaceLegalCockpitService);
        this.officeWorkspaceMainDashboardService = Objects.requireNonNull(officeWorkspaceMainDashboardService);
        this.officeWorkspaceExecutiveDashboardService = Objects.requireNonNull(officeWorkspaceExecutiveDashboardService);
        this.professionalForensicExecutiveDashboardService = Objects.requireNonNull(professionalForensicExecutiveDashboardService);
        this.professionalRoleExecutiveDashboardService = Objects.requireNonNull(professionalRoleExecutiveDashboardService);
        this.professionalOrganExecutiveDashboardService = Objects.requireNonNull(professionalOrganExecutiveDashboardService);
        this.officeWorkspaceTeamAvatarService = Objects.requireNonNull(officeWorkspaceTeamAvatarService);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
    }

    @Transactional(readOnly = true)
    public PjbFrontendCurrentUserView me(Authentication authentication) {
        Usuario usuario = currentUserService.getRequired();
        String assurance = govBrAssuranceExtractor.extract(authentication);
        PjbFrontendCurrentUserView view = new PjbFrontendCurrentUserView(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                maskCpf(usuario.getCpf()),
                usuario.getTipoUsuario() == null ? null : usuario.getTipoUsuario().name(),
                usuario.getPerfil(),
                usuario.getTipoUsuario() == null ? "CIDADAO" : usuario.getTipoUsuario().papelArquitetural(),
                usuario.getUf(),
                usuario.getComarca(),
                usuario.isAtivo(),
                assurance,
                govBrAssurancePolicy.exigeStepUp(assurance, true),
                authorities(authentication));
        auditLedgerService.appendSafely("FRONTEND_APP_ME_QUERY", "FRONTEND", String.valueOf(usuario.getId()), "tipo=" + view.tipoUsuario());
        return view;
    }

    @Transactional(readOnly = true)
    public PjbFrontendCapabilitySummaryView capabilities() {
        Usuario usuario = currentUserService.getRequired();
        CapabilityExtensionResponse response = capabilitySurfaceFacadeService.capacidades(null);
        List<String> capabilities = response.capabilities() == null ? List.of() : response.capabilities().stream().sorted().toList();
        PjbFrontendCapabilitySummaryView view = new PjbFrontendCapabilitySummaryView(
                response.role(),
                usuario.getTipoUsuario() == null ? "CIDADAO" : usuario.getTipoUsuario().papelArquitetural(),
                capabilities.size(),
                usuario.getTipoUsuario() != null && usuario.getTipoUsuario().isInstitucional(),
                usuario.getTipoUsuario() == null || usuario.getTipoUsuario().isCidadaniaExterna(),
                capabilities);
        auditLedgerService.appendSafely("FRONTEND_APP_CAPABILITIES_QUERY", "FRONTEND", String.valueOf(usuario.getId()), "count=" + view.capabilityCount());
        return view;
    }

    @Transactional(readOnly = true)
    public PjbFrontendContextView context(Authentication authentication, HttpServletRequest request) {
        Usuario usuario = currentUserService.getRequired();
        SecurityContextResponse securityContext = securityContextSurfaceFacadeService.context(request);
        String assurance = govBrAssuranceExtractor.extract(authentication);
        SecurityStateResponse security = securityContext.security();
        PjbAuthenticatedSessionResponse session = securityContext.institutionalSession();
        PjbFrontendContextView view = new PjbFrontendContextView(
                usuario.getTipoUsuario() == null ? null : usuario.getTipoUsuario().name(),
                usuario.getTipoUsuario() == null ? "CIDADAO" : usuario.getTipoUsuario().papelArquitetural(),
                activeHat(securityContext.hats()),
                session != null && session.authenticated(),
                session != null && session.jwtBacked(),
                session != null && session.trustedDeviceAtivo(),
                (session != null && session.contaGovBrVinculada()) || (security != null && security.govVerifiedAt() != null),
                assurance,
                govBrAssurancePolicy.exigeStepUp(assurance, true),
                security != null && security.frozen(),
                security == null || security.pendingSteps() == null ? 0 : security.pendingSteps().size(),
                security == null || security.pendingSteps() == null ? List.of() : List.copyOf(security.pendingSteps()),
                securityContext.hats() == null ? 0 : securityContext.hats().size(),
                session == null || session.authorities() == null ? authorities(authentication) : List.copyOf(session.authorities()),
                session == null ? null : session.landingPath());
        auditLedgerService.appendSafely("FRONTEND_APP_CONTEXT_QUERY", "FRONTEND", String.valueOf(usuario.getId()), "pending=" + view.pendingStepCount());
        return view;
    }

    @Transactional(readOnly = true)
    public List<PjbFrontendMenuItemView> menu(Authentication authentication) {
        Usuario usuario = currentUserService.getRequired();
        String assurance = govBrAssuranceExtractor.extract(authentication);
        List<PjbFrontendMenuItemView> menu = menuFor(usuario.getTipoUsuario(), assurance);
        auditLedgerService.appendSafely("FRONTEND_APP_MENU_QUERY", "FRONTEND", String.valueOf(usuario.getId()), "items=" + menu.size());
        return menu;
    }

    @Transactional(readOnly = true)
    public PjbFrontendSupportCatalogView supportCatalogs() {
        PjbFrontendSupportCatalogView view = new PjbFrontendSupportCatalogView(
                enumNames(TipoUsuario.values()),
                enumNames(RamoDireito.values()),
                enumNames(StatusProcesso.values()),
                enumNames(RitoProcessual.values()),
                List.of("PUBLIC", "AUTHENTICATED", "STEP_UP"),
                List.of("dashboard", "processos", "peticionamento", "seguranca", "perfil", "admin", "office-mode"));
        auditLedgerService.appendSafely("FRONTEND_APP_CATALOGS_QUERY", "FRONTEND", "CATALOGS", "tipos=" + view.tipoUsuarios().size());
        return view;
    }

    @Transactional(readOnly = true)
    public List<PjbFrontendRamoDireitoCatalogEntry> ramoDireitoCatalog() {
        List<PjbFrontendRamoDireitoCatalogEntry> view = java.util.Arrays.stream(RamoDireito.values())
                .sorted(Comparator.comparing(RamoDireito::getCodigo))
                .map(ramo -> new PjbFrontendRamoDireitoCatalogEntry(
                        ramo.getCodigo(),
                        ramo.name(),
                        ramo.getDescricao(),
                        ramo.getCategoria(),
                        ramo.verticalPrincipal(),
                        ramo.admiteConciliacao(),
                        ramo.exigeAtuacaoMP(),
                        ramo.geraSigiloAutomatico()))
                .toList();
        auditLedgerService.appendSafely("FRONTEND_APP_RAMO_CATALOG_QUERY", "FRONTEND", "CATALOGS", "ramos=" + view.size());
        return view;
    }

    @Transactional(readOnly = true)
    public List<PjbFrontendOwnedOfficeView> myOwnedOffices() {
        List<PjbFrontendOwnedOfficeView> view = officeWorkspaceCreationService.myOwnedOffices();
        Usuario usuario = currentUserService.getRequired();
        auditLedgerService.appendSafely("FRONTEND_APP_OWNED_OFFICES_QUERY", "FRONTEND", String.valueOf(usuario.getId()), "count=" + view.size());
        return view;
    }

    @Transactional
    public PjbFrontendOfficeModeView createOwnOffice(FrontendOfficeWorkspaceCreateRequest request) {
        return officeWorkspaceCreationService.createOwnOffice(request);
    }

    @Transactional
    public PjbFrontendOfficeModeView ensurePersonalOffice() {
        PjbFrontendOfficeModeView view = officeWorkspaceCreationService.ensurePersonalOffice();
        Usuario usuario = currentUserService.getRequired();
        auditLedgerService.appendSafely("FRONTEND_APP_PERSONAL_OFFICE_ENSURE", "FRONTEND", String.valueOf(usuario.getId()), "equipe=" + view.activeEquipeId());
        return view;
    }

    @Transactional(readOnly = true)
    public PjbFrontendOfficeWorkspaceSummaryView officeWorkspaceSummary(Long equipeId, HttpServletRequest request) {
        PjbFrontendOfficeWorkspaceSummaryView view = officeWorkspaceDashboardService.currentSummary(request, equipeId);
        Usuario usuario = currentUserService.getRequired();
        auditLedgerService.appendSafely("FRONTEND_APP_OFFICE_WORKSPACE_SUMMARY", "FRONTEND", String.valueOf(usuario.getId()), "equipe=" + (view == null ? null : view.equipeId()));
        return view;
    }

    @Transactional(readOnly = true)
    public PjbFrontendOfficeModeView officeMode(HttpServletRequest request) {
        return officeWorkspaceModeService.current(request);
    }

    @Transactional
    public PjbFrontendOfficeModeView updateOfficeMode(FrontendOfficeModeUpdateRequest request) {
        return officeWorkspaceModeService.update(request);
    }

    @Transactional
    public PjbFrontendOfficeModeView clearOfficeMode() {
        return officeWorkspaceModeService.clear();
    }


    @Transactional(readOnly = true)
    public List<com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeAffiliationInviteView> myIncomingOfficeInvites() {
        return officeAffiliationInviteService.myIncomingInvites();
    }

    @Transactional(readOnly = true)
    public List<com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeAffiliationInviteView> officeInvites(Long equipeId) {
        return officeAffiliationInviteService.officeInvites(equipeId);
    }

    @Transactional
    public com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeAffiliationInviteView createOfficeInvite(FrontendOfficeAffiliationInviteRequest request) {
        return officeAffiliationInviteService.createInvite(request);
    }

    @Transactional
    public PjbFrontendOfficeAffiliationDecisionResultView acceptOfficeInvite(Long inviteId, FrontendOfficeAffiliationDecisionRequest request) {
        return officeAffiliationInviteService.acceptInvite(inviteId, request);
    }

    @Transactional
    public PjbFrontendOfficeAffiliationDecisionResultView confirmOfficeInviteActivation(Long inviteId, FrontendOfficeAffiliationFinalApprovalRequest request) {
        return officeAffiliationInviteService.confirmInviteActivation(inviteId, request);
    }

    @Transactional
    public com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeAffiliationInviteView rejectOfficeInvite(Long inviteId) {
        return officeAffiliationInviteService.rejectInvite(inviteId);
    }

    @Transactional
    public com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeAffiliationInviteView revokeOfficeInvite(Long inviteId) {
        return officeAffiliationInviteService.revokeInvite(inviteId);
    }

    @Transactional(readOnly = true)
    public List<PjbFrontendOfficeProcessTransferView> myIncomingOfficeTransfers() {
        return officeProcessTransferService.myIncomingTransfers();
    }

    @Transactional(readOnly = true)
    public PjbFrontendOfficeProcessTransferPreviewView previewOfficeTransfer(FrontendOfficeProcessTransferRequest request) {
        return officeProcessTransferService.previewTransfer(request);
    }

    @Transactional(readOnly = true)
    public List<PjbFrontendOfficeProcessTransferView> officeTransfers(Long equipeId) {
        return officeProcessTransferService.officeTransfers(equipeId);
    }

    @Transactional
    public PjbFrontendOfficeProcessTransferView createOfficeTransfer(FrontendOfficeProcessTransferRequest request) {
        return officeProcessTransferService.createTransfer(request);
    }

    @Transactional
    public PjbFrontendOfficeProcessTransferView acceptOfficeTransfer(Long transferId, FrontendOfficeProcessTransferDecisionRequest request) {
        return officeProcessTransferService.acceptTransfer(transferId, request);
    }

    @Transactional
    public PjbFrontendOfficeProcessTransferView rejectOfficeTransfer(Long transferId) {
        return officeProcessTransferService.rejectTransfer(transferId);
    }

    @Transactional(readOnly = true)
    public PjbFrontendOfficeWorkspaceProcessPageView officeWorkspaceProcesses(FrontendOfficeWorkspaceProcessQueryRequest request, HttpServletRequest httpServletRequest) {
        return officeProcessWorkspaceScopeService.currentWorkspaceProcesses(request, httpServletRequest);
    }

    @Transactional(readOnly = true)
    public PjbFrontendOfficeProcessAccessView officeWorkspaceProcessAccess(Long processoId, String actionType, HttpServletRequest httpServletRequest) {
        return officeProcessWorkspaceScopeService.access(processoId, parseOfficeActionType(actionType), httpServletRequest);
    }

    @Transactional
    public PjbFrontendOfficeGovernedPetitionView submitOfficeGovernedPetition(Long processoId,
                                                                               FrontendOfficeGovernedPetitionRequest request,
                                                                               HttpServletRequest httpServletRequest) {
        return officeGovernedPetitionService.submit(processoId, request, httpServletRequest);
    }


    @Transactional(readOnly = true)
    public PjbFrontendOfficeQueuePageView officeWorkspaceQueue(Integer page, Integer size, String status) {
        int resolvedPage = page == null || page < 0 ? 0 : page;
        int resolvedSize = size == null || size < 1 ? 20 : Math.min(size, 100);
        OfficeQueueStatus resolvedStatus = parseOfficeQueueStatus(status);
        var resultPage = officeSignatureQueueService.listarPorSigner(currentUserService.currentUserIdOrZero(), resolvedStatus, PageRequest.of(resolvedPage, resolvedSize));
        return new PjbFrontendOfficeQueuePageView(
                resultPage.getNumber(),
                resultPage.getSize(),
                resultPage.getTotalElements(),
                resultPage.getTotalPages(),
                resolvedStatus.name(),
                resultPage.getContent().stream().map(this::toFrontendQueueItem).toList()
        );
    }

    @Transactional
    public PjbFrontendOfficeQueueItemView approveOfficeWorkspaceQueue(Long queueItemId, FrontendOfficeQueueDecisionRequest request) {
        return toFrontendQueueItem(officeSignatureQueueService.aprovar(currentUserService.currentUserIdOrZero(), queueItemId, request == null ? null : request.reason()));
    }

    @Transactional
    public PjbFrontendOfficeQueueItemView rejectOfficeWorkspaceQueue(Long queueItemId, FrontendOfficeQueueDecisionRequest request) {
        return toFrontendQueueItem(officeSignatureQueueService.rejeitar(currentUserService.currentUserIdOrZero(), queueItemId, request == null ? null : request.reason()));
    }

    @Transactional(readOnly = true)
    public PjbFrontendOfficeGovernedDocumentBatchPreviewView previewOfficeGovernedDocumentBatch(Long processoId, UUID batchId, HttpServletRequest httpServletRequest) {
        return officeGovernedDocumentFilingService.preview(processoId, batchId, httpServletRequest);
    }

    @Transactional
    public PjbFrontendOfficeGovernedDocumentBatchLinkView linkOfficeGovernedDocumentBatch(Long processoId,
                                                                                           FrontendOfficeGovernedDocumentBatchLinkRequest request,
                                                                                           HttpServletRequest httpServletRequest) {
        return officeGovernedDocumentFilingService.linkBatch(processoId, request, httpServletRequest);
    }

    @Transactional
    public PjbFrontendOfficeGovernedProtocolSubmissionView submitOfficeGovernedProtocol(Long processoId,
                                                                                         Long protocolPackageId,
                                                                                         FrontendOfficeGovernedProtocolSubmitRequest request,
                                                                                         HttpServletRequest httpServletRequest) {
        return officeGovernedExternalProtocolService.submit(processoId, protocolPackageId, request, httpServletRequest);
    }

    @Transactional
    public PjbFrontendOfficeGovernedUploadBatchView createOfficeGovernedUploadBatch(Long processoId,
                                                                                     FrontendOfficeGovernedUploadBatchCreateRequest request,
                                                                                     HttpServletRequest httpServletRequest) {
        return officeGovernedUploadIngressService.createBatch(processoId, request, httpServletRequest);
    }

    @Transactional(readOnly = true)
    public PjbFrontendOfficeGovernedUploadBatchView officeGovernedUploadBatch(Long processoId, UUID batchId, HttpServletRequest httpServletRequest) {
        return officeGovernedUploadIngressService.batch(processoId, batchId, httpServletRequest);
    }

    @Transactional
    public PjbFrontendOfficeGovernedUploadItemReservationView reserveOfficeGovernedUploadItem(Long processoId,
                                                                                               UUID batchId,
                                                                                               FrontendOfficeGovernedUploadReserveItemRequest request,
                                                                                               HttpServletRequest httpServletRequest) {
        return officeGovernedUploadIngressService.reserveItem(processoId, batchId, request, httpServletRequest);
    }

    @Transactional
    public PjbFrontendOfficeGovernedUploadIngressView directOfficeGovernedUpload(Long processoId,
                                                                                  UUID batchId,
                                                                                  UUID itemId,
                                                                                  String token,
                                                                                  HttpServletRequest httpServletRequest) throws Exception {
        return officeGovernedUploadIngressService.directUpload(processoId, batchId, itemId, token, httpServletRequest);
    }

    @Transactional
    public PjbFrontendOfficeGovernedUploadFinalizeView finalizeOfficeGovernedUploadBatch(Long processoId,
                                                                                          UUID batchId,
                                                                                          FrontendOfficeGovernedUploadFinalizeRequest request,
                                                                                          HttpServletRequest httpServletRequest) {
        return officeGovernedUploadIngressService.finalizeBatch(processoId, batchId, request, httpServletRequest);
    }

    @Transactional(readOnly = true)
    public PjbFrontendOfficeGovernedMultimediaWorkspaceView previewOfficeGovernedMultimediaWorkspace(Long processoId,
                                                                                                      FrontendOfficeGovernedMultimediaWorkspaceRequest request,
                                                                                                      HttpServletRequest httpServletRequest) {
        return officeGovernedMultimediaWorkspaceService.preview(processoId, request, httpServletRequest);
    }


@Transactional(readOnly = true)
public PjbFrontendOfficeWorkspaceLegalCockpitView officeWorkspaceLegalCockpit(Authentication authentication,
                                                                              HttpServletRequest request,
                                                                              java.time.LocalDate from,
                                                                              java.time.LocalDate to,
                                                                              Long processoId) {
    PjbFrontendOfficeWorkspaceLegalCockpitView view = officeWorkspaceLegalCockpitService.cockpit(authentication, request, from, to, processoId);
    Usuario usuario = currentUserService.getRequired();
    auditLedgerService.appendSafely("FRONTEND_APP_OFFICE_LEGAL_COCKPIT", "FRONTEND", String.valueOf(usuario.getId()), "equipe=" + view.activeEquipeId() + " processo=" + processoId);
    return view;
}

@Transactional(readOnly = true)
public PjbFrontendOfficeProcessReadingModeView officeProcessReadingMode(Long processoId,
                                                                        HttpServletRequest request,
                                                                        java.time.LocalDate from,
                                                                        java.time.LocalDate to) {
    PjbFrontendOfficeProcessReadingModeView view = officeWorkspaceLegalCockpitService.readingMode(processoId, request, from, to);
    Usuario usuario = currentUserService.getRequired();
    auditLedgerService.appendSafely("FRONTEND_APP_OFFICE_PROCESS_READING_MODE", "PROCESSO", String.valueOf(processoId), "usuario=" + usuario.getId());
    return view;
}

@Transactional(readOnly = true)
public PjbFrontendOfficeWorkspaceMainDashboardView officeWorkspaceMainDashboard(Authentication authentication,
                                                                                HttpServletRequest request,
                                                                                java.time.LocalDate from,
                                                                                java.time.LocalDate to) {
    PjbFrontendOfficeWorkspaceMainDashboardView view = officeWorkspaceMainDashboardService.dashboard(authentication, request, from, to);
    Usuario usuario = currentUserService.getRequired();
    auditLedgerService.appendSafely("FRONTEND_APP_OFFICE_MAIN_DASHBOARD", "FRONTEND", String.valueOf(usuario.getId()), "equipe=" + view.activeEquipeId());
    return view;
}

@Transactional(readOnly = true)
public PjbFrontendOfficeWorkspaceExecutiveDashboardView officeWorkspaceExecutiveDashboard(Authentication authentication,
                                                                                         HttpServletRequest request,
                                                                                         java.time.LocalDate from,
                                                                                         java.time.LocalDate to) {
    PjbFrontendOfficeWorkspaceExecutiveDashboardView view = officeWorkspaceExecutiveDashboardService.dashboard(authentication, request, from, to);
    Usuario usuario = currentUserService.getRequired();
    auditLedgerService.appendSafely("FRONTEND_APP_OFFICE_EXECUTIVE_DASHBOARD", "FRONTEND", String.valueOf(usuario.getId()), "equipe=" + view.activeEquipeId());
    return view;
}

@Transactional(readOnly = true)
public PjbFrontendProfessionalWorkspaceExecutiveDashboardView professionalWorkspaceExecutiveDashboard(Authentication authentication,
                                                                                                    java.time.LocalDate from,
                                                                                                    java.time.LocalDate to) {
    PjbFrontendProfessionalWorkspaceExecutiveDashboardView view = professionalForensicExecutiveDashboardService.dashboard(authentication, from, to);
    Usuario usuario = currentUserService.getRequired();
    auditLedgerService.appendSafely("FRONTEND_APP_PROFESSIONAL_EXECUTIVE_DASHBOARD", "FRONTEND", String.valueOf(usuario.getId()), "actorClass=" + view.actorClass());
    return view;
}

@Transactional(readOnly = true)
public PjbFrontendProfessionalRoleExecutiveDashboardView magistratureExecutiveDashboard(Authentication authentication,
                                                                                        java.time.LocalDate from,
                                                                                        java.time.LocalDate to) {
    PjbFrontendProfessionalRoleExecutiveDashboardView view = professionalRoleExecutiveDashboardService.magistratureDashboard(authentication, from, to);
    Usuario usuario = currentUserService.getRequired();
    auditLedgerService.appendSafely("FRONTEND_APP_MAGISTRATURE_EXECUTIVE_DASHBOARD", "FRONTEND", String.valueOf(usuario.getId()), "kind=" + view.dashboardKind());
    return view;
}

@Transactional(readOnly = true)
public PjbFrontendProfessionalRoleExecutiveDashboardView defensoriaExecutiveDashboard(Authentication authentication,
                                                                                      java.time.LocalDate from,
                                                                                      java.time.LocalDate to) {
    PjbFrontendProfessionalRoleExecutiveDashboardView view = professionalRoleExecutiveDashboardService.defensoriaDashboard(authentication, from, to);
    Usuario usuario = currentUserService.getRequired();
    auditLedgerService.appendSafely("FRONTEND_APP_DEFENSORIA_EXECUTIVE_DASHBOARD", "FRONTEND", String.valueOf(usuario.getId()), "kind=" + view.dashboardKind());
    return view;
}

@Transactional(readOnly = true)
public PjbFrontendProfessionalRoleExecutiveDashboardView procuradoriaExecutiveDashboard(Authentication authentication,
                                                                                        java.time.LocalDate from,
                                                                                        java.time.LocalDate to) {
    PjbFrontendProfessionalRoleExecutiveDashboardView view = professionalRoleExecutiveDashboardService.procuradoriaDashboard(authentication, from, to);
    Usuario usuario = currentUserService.getRequired();
    auditLedgerService.appendSafely("FRONTEND_APP_PROCURADORIA_EXECUTIVE_DASHBOARD", "FRONTEND", String.valueOf(usuario.getId()), "kind=" + view.dashboardKind());
    return view;
}

@Transactional(readOnly = true)
public PjbFrontendProfessionalOrganExecutiveDashboardView professionalOrganizationalExecutiveDashboard(Authentication authentication,
                                                                                                       java.time.LocalDate from,
                                                                                                       java.time.LocalDate to) {
    PjbFrontendProfessionalOrganExecutiveDashboardView view = professionalOrganExecutiveDashboardService.dashboard(authentication, from, to);
    Usuario usuario = currentUserService.getRequired();
    auditLedgerService.appendSafely("FRONTEND_APP_PROFESSIONAL_ORGAN_EXECUTIVE_DASHBOARD", "FRONTEND", String.valueOf(usuario.getId()), "kind=" + view.dashboardKind());
    return view;
}

@Transactional(readOnly = true)
public PjbFrontendProfessionalOrganExecutiveDashboardView magistratureOrganExecutiveDashboard(Authentication authentication,
                                                                                              java.time.LocalDate from,
                                                                                              java.time.LocalDate to) {
    PjbFrontendProfessionalOrganExecutiveDashboardView view = professionalOrganExecutiveDashboardService.magistratureDashboard(authentication, from, to);
    Usuario usuario = currentUserService.getRequired();
    auditLedgerService.appendSafely("FRONTEND_APP_MAGISTRATURE_ORGAN_EXECUTIVE_DASHBOARD", "FRONTEND", String.valueOf(usuario.getId()), "kind=" + view.dashboardKind());
    return view;
}

@Transactional(readOnly = true)
public PjbFrontendProfessionalOrganExecutiveDashboardView defensoriaOrganExecutiveDashboard(Authentication authentication,
                                                                                            java.time.LocalDate from,
                                                                                            java.time.LocalDate to) {
    PjbFrontendProfessionalOrganExecutiveDashboardView view = professionalOrganExecutiveDashboardService.defensoriaDashboard(authentication, from, to);
    Usuario usuario = currentUserService.getRequired();
    auditLedgerService.appendSafely("FRONTEND_APP_DEFENSORIA_ORGAN_EXECUTIVE_DASHBOARD", "FRONTEND", String.valueOf(usuario.getId()), "kind=" + view.dashboardKind());
    return view;
}

@Transactional(readOnly = true)
public PjbFrontendProfessionalOrganExecutiveDashboardView procuradoriaOrganExecutiveDashboard(Authentication authentication,
                                                                                              java.time.LocalDate from,
                                                                                              java.time.LocalDate to) {
    PjbFrontendProfessionalOrganExecutiveDashboardView view = professionalOrganExecutiveDashboardService.procuradoriaDashboard(authentication, from, to);
    Usuario usuario = currentUserService.getRequired();
    auditLedgerService.appendSafely("FRONTEND_APP_PROCURADORIA_ORGAN_EXECUTIVE_DASHBOARD", "FRONTEND", String.valueOf(usuario.getId()), "kind=" + view.dashboardKind());
    return view;
}

@Transactional(readOnly = true)
public OfficeBinaryPayload officeTeamMemberAvatar(Long userId, HttpServletRequest request) throws java.io.IOException {
    ObjectReadResult rr = officeWorkspaceTeamAvatarService.read(userId, request);
    byte[] bytes = rr.resource().getInputStream().readAllBytes();
    Usuario usuario = currentUserService.getRequired();
    auditLedgerService.appendSafely("FRONTEND_APP_OFFICE_TEAM_AVATAR", "FRONTEND", String.valueOf(usuario.getId()), "target=" + userId);
    return new OfficeBinaryPayload(rr.contentType(), bytes);
}

    @Transactional(readOnly = true)
    public PjbFrontendAppBootstrapView bootstrap(Authentication authentication, HttpServletRequest request) {
        PjbFrontendCurrentUserView me = me(authentication);
        PjbFrontendContextView context = context(authentication, request);
        PjbFrontendCapabilitySummaryView capabilities = capabilities();
        List<PjbFrontendMenuItemView> menu = menu(authentication);
        PjbFrontendSupportCatalogView catalogs = supportCatalogs();
        PjbFrontendOfficeModeView officeMode = officeMode(request);
        PjbFrontendOfficeWorkspaceSummaryView officeWorkspaceSummary = officeWorkspaceSummary(null, request);
        List<String> nextApiCalls = suggestedCalls(me.tipoUsuario(), officeMode, officeWorkspaceSummary);
        PjbFrontendAppBootstrapView view = new PjbFrontendAppBootstrapView(me, context, capabilities, menu, catalogs, officeMode, officeWorkspaceSummary, nextApiCalls);
        auditLedgerService.appendSafely("FRONTEND_APP_BOOTSTRAP_QUERY", "FRONTEND", String.valueOf(me.userId()), "menu=" + menu.size());
        return view;
    }

    private List<PjbFrontendMenuItemView> menuFor(TipoUsuario tipoUsuario, String assurance) {
        ArrayList<PjbFrontendMenuItemView> items = new ArrayList<>();
        if (tipoUsuario == null || tipoUsuario == TipoUsuario.CIDADAO || tipoUsuario.isCidadaniaExterna()) {
            items.add(menu("dashboard", "Painel do cidadão", "/api/v1/cidadao/painel", "dashboard", "prata", false));
            items.add(menu("processos", "Meus processos", "/api/v1/cidadao/processos", "processos", "prata", false));
            items.add(menu("timeline", "Timeline simplificada", "/api/v1/cidadao/dashboard-enhanced/snapshot", "processos", "prata", false));
            items.add(menu("perfil", "Meu perfil", "/api/v1/cidadao/perfil", "perfil", "prata", false));
        } else if (tipoUsuario.isAdvocacia() || tipoUsuario.isDefensoriaPublica() || tipoUsuario.isProcuradoria() || tipoUsuario.isMinisterioPublico()) {
            items.add(menu("dashboard", "Painel profissional", "/api/v1/advogado/dashboard", "dashboard", "prata", false));
            items.add(menu("cockpit", "Cockpit processual", "/api/v1/advogado/cockpit", "processos", "prata", false));
            items.add(menu("office-mode", "Modo escritório", "/api/v1/frontend/app/office-mode", "processos", "prata", false));
            items.add(menu("office-executive", "Dashboard executivo do escritório", "/api/v1/frontend/app/offices/workspace/executive-dashboard", "dashboard", "prata", false));
            items.add(menu("professional-executive", "Dashboard executivo profissional", "/api/v1/frontend/app/professional/workspace/executive-dashboard", "dashboard", "prata", false));
            if (tipoUsuario.isDefensoriaPublica()) {
                items.add(menu("defensoria-executive", "Dashboard executivo da defensoria", "/api/v1/frontend/app/professional/workspace/defensoria-executive-dashboard", "dashboard", "prata", false));
                items.add(menu("defensoria-organ", "Painel institucional da defensoria", "/api/v1/frontend/app/professional/workspace/defensoria-organ-dashboard", "dashboard", "prata", false));
            }
            if (tipoUsuario.isProcuradoria()) {
                items.add(menu("procuradoria-executive", "Dashboard executivo da procuradoria", "/api/v1/frontend/app/professional/workspace/procuradoria-executive-dashboard", "dashboard", "prata", false));
                items.add(menu("procuradoria-organ", "Painel institucional da procuradoria", "/api/v1/frontend/app/professional/workspace/procuradoria-organ-dashboard", "dashboard", "prata", false));
            }
            items.add(menu("peticionamento", "Peticionamento", "/api/v1/peticionamento", "peticionamento", "prata", false));
            items.add(menu("prazos", "Prazos", "/api/v1/processual/prazos", "processos", "prata", false));
        } else if (tipoUsuario.isMagistratura()) {
            items.add(menu("contexto", "Contexto magistratura", "/api/v1/magistratura/context", "dashboard", "prata", false));
            items.add(menu("processos", "Processos da magistratura", "/api/v1/magistratura/processos", "processos", "prata", false));
            items.add(menu("unificado", "Painel processual unificado", "/api/v1/processual/unificado", "processos", "prata", false));
            items.add(menu("professional-executive", "Dashboard executivo profissional", "/api/v1/frontend/app/professional/workspace/executive-dashboard", "dashboard", "prata", false));
            items.add(menu("magistrature-executive", "Dashboard executivo da magistratura", "/api/v1/frontend/app/professional/workspace/magistrature-executive-dashboard", "dashboard", "prata", false));
            items.add(menu("magistrature-organ", "Painel institucional do gabinete", "/api/v1/frontend/app/professional/workspace/magistrature-organ-dashboard", "dashboard", "prata", false));
            items.add(menu("seguranca", "Assurance Gov.br", "/api/v1/security/context", "seguranca", "ouro", govBrAssurancePolicy.exigeStepUp(assurance, true)));
        } else if (tipoUsuario.isAdministradorSistema()) {
            items.add(menu("final-closure", "Fechamento final", "/api/v1/admin/final-closure/summary", "admin", "prata", false));
            items.add(menu("frontend-readiness", "Readiness frontend", "/api/v1/admin/frontend-readiness/summary", "admin", "prata", false));
            items.add(menu("quality-gates", "Quality gates", "/api/v1/admin/quality-gates/summary", "admin", "prata", false));
            items.add(menu("runtime", "Runtime", "/api/v1/admin/runtime/health", "admin", "prata", false));
        } else {
            items.add(menu("processos", "Processos", "/api/v1/processos", "processos", "prata", false));
            items.add(menu("timeline", "Timeline", "/api/v1/processual/unificado", "processos", "prata", false));
            items.add(menu("seguranca", "Contexto de segurança", "/api/v1/security/context", "seguranca", "prata", false));
            items.add(menu("perfil", "Capacidades", "/api/v1/profile/capabilities-extension", "perfil", "prata", false));
        }
        return items;
    }


    private PjbFrontendOfficeQueueItemView toFrontendQueueItem(OfficeQueueItemDto source) {
        return new PjbFrontendOfficeQueueItemView(
                source.getId(),
                source.getEquipeId(),
                source.getExecutorUserId(),
                source.getSignerUserId(),
                source.getActionType() == null ? null : source.getActionType().name(),
                source.getResourceType(),
                source.getResourceId(),
                source.getStatus() == null ? null : source.getStatus().name(),
                source.getCreatedAt(),
                source.getDecidedAt(),
                source.getDecidedByUserId(),
                source.getDecisionReason(),
                source.getRequestId(),
                source.getPayloadHash(),
                source.getSummary()
        );
    }

    private OfficeQueueStatus parseOfficeQueueStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return OfficeQueueStatus.PENDING;
        }
        try {
            return OfficeQueueStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return OfficeQueueStatus.PENDING;
        }
    }

    private PjbFrontendMenuItemView menu(String code, String label, String path, String domain, String requiredAssuranceLevel, boolean requiresStepUp) {
        return new PjbFrontendMenuItemView(code, label, path, domain, requiredAssuranceLevel, requiresStepUp, "dashboard".equals(code) || "processos".equals(code) || "peticionamento".equals(code) || "office-mode".equals(code));
    }

    private List<String> suggestedCalls(String tipoUsuario, PjbFrontendOfficeModeView officeMode, PjbFrontendOfficeWorkspaceSummaryView officeWorkspaceSummary) {
        if (tipoUsuario == null || "CIDADAO".equals(tipoUsuario)) {
            return List.of(
                    "/api/v1/frontend/app/me",
                    "/api/v1/frontend/app/me/menu",
                    "/api/v1/cidadao/painel",
                    "/api/v1/cidadao/dashboard-enhanced/snapshot",
                    "/api/v1/cidadao/processos"
            );
        }
        ArrayList<String> calls = new ArrayList<>(List.of(
                "/api/v1/frontend/app/me",
                "/api/v1/frontend/app/me/capabilities",
                "/api/v1/frontend/app/me/context",
                "/api/v1/frontend/app/support/catalogs",
                "/api/v1/processos"
        ));
        if (officeMode != null && !officeMode.memberships().isEmpty()) {
            calls.add("/api/v1/frontend/app/office-mode");
            calls.add("/api/v1/frontend/app/offices/workspace/summary");
            calls.add("/api/v1/frontend/app/offices/workspace/processes/query");
            calls.add("/api/v1/frontend/app/offices/workspace/legal-cockpit");
            calls.add("/api/v1/frontend/app/offices/workspace/main-dashboard");
            calls.add("/api/v1/frontend/app/offices/workspace/executive-dashboard");
        }
        TipoUsuario resolvedTipo = TipoUsuario.fromString(tipoUsuario);
        if (resolvedTipo != null && (resolvedTipo.isAdvocacia() || resolvedTipo.isDefensoriaPublica() || resolvedTipo.isProcuradoria() || resolvedTipo.isMagistratura() || resolvedTipo.isAssessor() || resolvedTipo.isMinisterioPublico() || resolvedTipo.isServidorJudiciario())) {
            calls.add("/api/v1/frontend/app/professional/workspace/executive-dashboard");
        }
        if (resolvedTipo != null && (resolvedTipo.isMagistratura() || resolvedTipo.isAssessor() || resolvedTipo.isServidorJudiciario())) {
            calls.add("/api/v1/frontend/app/professional/workspace/magistrature-executive-dashboard");
            calls.add("/api/v1/frontend/app/professional/workspace/magistrature-organ-dashboard");
        }
        if (resolvedTipo != null && resolvedTipo.isDefensoriaPublica()) {
            calls.add("/api/v1/frontend/app/professional/workspace/defensoria-executive-dashboard");
            calls.add("/api/v1/frontend/app/professional/workspace/defensoria-organ-dashboard");
        }
        if (resolvedTipo != null && resolvedTipo.isProcuradoria()) {
            calls.add("/api/v1/frontend/app/professional/workspace/procuradoria-executive-dashboard");
            calls.add("/api/v1/frontend/app/professional/workspace/procuradoria-organ-dashboard");
        }
        if (officeWorkspaceSummary == null && officeMode != null && "PERSONAL".equalsIgnoreCase(officeMode.mode())) {
            calls.add("/api/v1/frontend/app/offices/personal/ensure");
        }
        return List.copyOf(calls);
    }

    private com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeActionType parseOfficeActionType(String actionType) {
        if (actionType == null || actionType.isBlank()) {
            return null;
        }
        try {
            return com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeActionType.valueOf(actionType.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public record OfficeBinaryPayload(String contentType, byte[] bytes) {}

    private String activeHat(List<SecurityHatResponse> hats) {
        if (hats == null || hats.isEmpty()) {
            return "INDEPENDENTE";
        }
        return hats.get(0).equipeNome();
    }

    private List<String> authorities(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return List.of();
        }
        return authentication.getAuthorities().stream()
                .map(authority -> authority == null ? null : authority.getAuthority())
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    private List<String> enumNames(Enum<?>[] values) {
        return java.util.Arrays.stream(values).map(Enum::name).sorted().toList();
    }

    private String maskCpf(String cpf) {
        if (cpf == null || cpf.isBlank()) {
            return null;
        }
        String digits = cpf.replaceAll("\\D", "");
        if (digits.length() != 11) {
            return "***";
        }
        return "***." + digits.substring(3, 6) + "." + digits.substring(6, 9) + "-**";
    }
}
