package com.tcc.pjb.backend.controller.ui;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.frontend.app.application.PjbFrontendAppApplicationService;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendAppBootstrapView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendCapabilitySummaryView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendContextView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendCurrentUserView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendMenuItemView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeAffiliationDecisionResultView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeAffiliationInviteView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeMembershipView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeModeView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeWorkspaceSummaryView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeWorkspaceLegalCockpitView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeWorkspaceMainDashboardKpiView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeWorkspaceMainDashboardView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeCriticalDeadlineView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficePendingPetitionView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeProcessReadingModeView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeWorkspaceProcessCardView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeTeamMemberView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeGovernedDocumentBatchLinkView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeGovernedPetitionView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeGovernedDocumentBatchPreviewView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeGovernedProtocolSubmissionView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeGovernedMultimediaWorkspaceView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeGovernedUploadBatchView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeGovernedUploadFinalizeView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeGovernedUploadIngressView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeGovernedUploadItemReservationView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeProcessAccessView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeQueueItemView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeQueuePageView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeWorkspaceProcessPageView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeWorkspaceProcessView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeProcessTransferImpactItemView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeProcessTransferPreviewView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeProcessTransferView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOwnedOfficeView;
import com.tcc.pjb.backend.model.dto.calendar.CalendarWorkspaceResponse;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialWorkspaceResponse;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialSolicitantePerfil;
import com.tcc.pjb.backend.model.dto.timeline.TimelineItemResponse;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendRamoDireitoCatalogEntry;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendSupportCatalogView;
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
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeProcessTransferRequest;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeQueueDecisionRequest;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeWorkspaceCreateRequest;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeWorkspaceProcessQueryRequest;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class FrontendAppControllerTest {

    private MockMvc mockMvc;
    private PjbFrontendAppApplicationService applicationService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        applicationService = mock(PjbFrontendAppApplicationService.class);
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(new FrontendAppController(applicationService, new ApiResponseFactory())).build();
    }

    @Test
    void endpoints_devemExporContextoFinalDoFrontend() throws Exception {
        PjbFrontendOfficeModeView officeMode = new PjbFrontendOfficeModeView(
                "HYBRID",
                44L,
                "Escritorio Rocha & Silva",
                77L,
                "Dr. Senior",
                true,
                true,
                true,
                false,
                false,
                false,
                List.of(new PjbFrontendOfficeMembershipView(44L, "Escritorio Rocha & Silva", "ADVOGADO_JUNIOR", "Associado", 77L, "Dr. Senior", true, false, true, true, true, List.of("CIVIL"), false, 6, "ELEVADO", 8, true, 10)),
                List.of("Pode abrir causas proprias sem perder o contexto do escritorio."),
                List.of("CIVIL"),
                false,
                6,
                "ELEVADO",
                8,
                true,
                77L,
                "Dr. Senior");

        PjbFrontendOfficeAffiliationInviteView inviteView = new PjbFrontendOfficeAffiliationInviteView(
                91L,
                44L,
                "Escritorio Rocha & Silva",
                10L,
                "Tiago Silva",
                "PENDING",
                "Maria Lima",
                "ma***@email.com",
                "***.456.789-**",
                "12345/CE",
                "ADVOGADO_JUNIOR",
                "Associada",
                List.of("CIVIL"),
                false,
                8,
                40,
                false,
                true,
                "HYBRID",
                10,
                true,
                "ouro",
                false,
                true,
                true,
                Instant.parse("2026-04-13T10:00:00Z"),
                null,
                Instant.parse("2026-05-13T10:00:00Z"),
                Instant.parse("2026-04-16T10:00:00Z"));

        PjbFrontendOfficeAffiliationDecisionResultView decisionResult = new PjbFrontendOfficeAffiliationDecisionResultView(inviteView, officeMode, true, false);

        PjbFrontendOfficeProcessTransferPreviewView transferPreviewView = new PjbFrontendOfficeProcessTransferPreviewView(
                44L,
                "Escritorio Rocha & Silva",
                45L,
                "Escritorio Lima",
                99L,
                "Maria Lima",
                2,
                1,
                true,
                true,
                "Preview de transferencia de 2 processo(s).",
                "preview-hash-501",
                8,
                "ELEVADO",
                8,
                false,
                List.of("CIVIL"),
                List.of(),
                List.of("ASSINATURA_PATRONAL_OBRIGATORIA"),
                List.of(new PjbFrontendOfficeProcessTransferImpactItemView(1001L, "0001", "CIVIL", "PUBLICO", false, false, true, List.of(), List.of("ASSINATURA_PATRONAL_OBRIGATORIA")))
        );

        PjbFrontendOfficeProcessTransferView transferView = new PjbFrontendOfficeProcessTransferView(
                501L,
                44L,
                "Escritorio Rocha & Silva",
                45L,
                "Escritorio Lima",
                99L,
                "Maria Lima",
                "PENDING_DESTINATION_ACCEPTANCE",
                2,
                1,
                "Redistribuicao formal",
                "Carteira civel",
                List.of(1001L, 1002L),
                "Transferencia formal de 2 processo(s).",
                true,
                true,
                Instant.parse("2026-04-13T10:00:00Z"),
                null,
                null);


        PjbFrontendOfficeWorkspaceProcessPageView workspaceProcessPageView = new PjbFrontendOfficeWorkspaceProcessPageView(
                "HYBRID",
                44L,
                "Escritorio Rocha & Silva",
                false,
                false,
                List.of("CIVIL"),
                6,
                8,
                0,
                20,
                1,
                1,
                List.of(),
                List.of("PROCESSOS_SENSIVEIS_OCULTADOS_POR_TRUST"),
                List.of(new PjbFrontendOfficeWorkspaceProcessView(1001L, "0001", 44L, "Escritorio Rocha & Silva", 10L, "Tiago Silva", "CIVIL", "PUBLICO", "EM_ANDAMENTO", true, true, false, false, true, List.of(), List.of("ASSINATURA_PATRONAL_OBRIGATORIA")))
        );

        PjbFrontendOfficeProcessAccessView workspaceProcessAccessView = new PjbFrontendOfficeProcessAccessView(
                1001L,
                "0001",
                44L,
                "HYBRID",
                "PETICIONAR",
                true,
                true,
                false,
                77L,
                "Dr. Senior",
                List.of(),
                List.of("ASSINATURA_PATRONAL_OBRIGATORIA")
        );

        PjbFrontendOfficeQueuePageView queuePageView = new PjbFrontendOfficeQueuePageView(
                0,
                20,
                1,
                1,
                "PENDING",
                List.of(new PjbFrontendOfficeQueueItemView(801L, 44L, 10L, 77L, "JUNTAR_DOCUMENTO", "ADV_PROCESS_OPERATION", "901", "PENDING", java.time.LocalDateTime.parse("2026-04-13T10:00:00"), null, null, null, "req-1", "hash-1", "JUNTADA_DOCUMENTAL"))
        );

        PjbFrontendOfficeGovernedDocumentBatchPreviewView documentBatchPreviewView = new PjbFrontendOfficeGovernedDocumentBatchPreviewView(
                1001L,
                java.util.UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "INITIATED",
                2,
                2,
                2,
                0,
                0,
                0,
                4096,
                "batch-fingerprint-1",
                "HYBRID",
                44L,
                true,
                true,
                77L,
                "Dr. Senior",
                true,
                List.of(),
                List.of("ASSINATURA_PATRONAL_OBRIGATORIA")
        );

        PjbFrontendOfficeGovernedDocumentBatchLinkView documentBatchLinkView = new PjbFrontendOfficeGovernedDocumentBatchLinkView(
                1001L,
                java.util.UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "PENDING_SIGNER",
                901L,
                801L,
                77L,
                null,
                null,
                List.of(),
                "batch-fingerprint-1",
                true,
                77L,
                "Dr. Senior",
                List.of("ASSINATURA_PATRONAL_OBRIGATORIA"),
                List.of()
        );

        PjbFrontendOfficeGovernedProtocolSubmissionView governedProtocolSubmissionView = new PjbFrontendOfficeGovernedProtocolSubmissionView(
                1001L,
                3001L,
                "integrity-1",
                "PENDING_SIGNER",
                802L,
                null,
                77L,
                true,
                null,
                null,
                null,
                "GREEN",
                true,
                List.of(),
                List.of("ASSINATURA_PATRONAL_OBRIGATORIA")
        );


        PjbFrontendOfficeGovernedUploadBatchView uploadBatchView = new PjbFrontendOfficeGovernedUploadBatchView(
                1001L,
                java.util.UUID.fromString("22222222-2222-2222-2222-222222222222"),
                "INITIATED",
                3,
                1,
                1,
                0,
                0,
                0,
                2048,
                "upload-batch-fingerprint-1",
                "HYBRID",
                44L,
                true,
                true,
                List.of(),
                List.of()
        );

        PjbFrontendOfficeGovernedUploadItemReservationView uploadReservationView = new PjbFrontendOfficeGovernedUploadItemReservationView(
                1001L,
                java.util.UUID.fromString("22222222-2222-2222-2222-222222222222"),
                java.util.UUID.fromString("33333333-3333-3333-3333-333333333333"),
                "/api/v1/frontend/app/offices/workspace/processes/1001/uploads/direct/22222222-2222-2222-2222-222222222222/33333333-3333-3333-3333-333333333333?token=tkn",
                "RESERVED",
                "upload-batch-fingerprint-2",
                true,
                List.of(),
                List.of()
        );

        PjbFrontendOfficeGovernedUploadIngressView uploadIngressView = new PjbFrontendOfficeGovernedUploadIngressView(
                1001L,
                java.util.UUID.fromString("22222222-2222-2222-2222-222222222222"),
                java.util.UUID.fromString("33333333-3333-3333-3333-333333333333"),
                "UPLOADED",
                "sha256-1",
                "sha384-1",
                "batches/22222222-2222-2222-2222-222222222222/33333333-3333-3333-3333-333333333333.pdf",
                "upload-batch-fingerprint-3",
                true,
                List.of(),
                List.of()
        );

        PjbFrontendOfficeGovernedUploadFinalizeView uploadFinalizeView = new PjbFrontendOfficeGovernedUploadFinalizeView(
                1001L,
                java.util.UUID.fromString("22222222-2222-2222-2222-222222222222"),
                java.util.UUID.fromString("44444444-4444-4444-4444-444444444444"),
                "PENDING",
                false,
                false,
                "upload-batch-fingerprint-3",
                true,
                List.of(),
                List.of()
        );

        PjbFrontendOfficeGovernedMultimediaWorkspaceView multimediaWorkspaceView = new PjbFrontendOfficeGovernedMultimediaWorkspaceView(
                1001L,
                "PETICIONAR",
                "INSTITUCIONAL",
                "PETICAO_INSTITUCIONAL",
                true,
                true,
                "HYBRID",
                44L,
                77L,
                "Dr. Senior",
                List.of(),
                List.of("ASSINATURA_PATRONAL_OBRIGATORIA"),
                "SUBMETER_PECA_INSTITUCIONAL",
                "PECA_INSTITUCIONAL_MULTIMIDIA",
                true,
                java.util.Map.of("nextAction", "SUBMETER_PECA_INSTITUCIONAL", "multimediaEnabled", true)
        );

        when(applicationService.me(org.mockito.ArgumentMatchers.any())).thenReturn(new PjbFrontendCurrentUserView(10L, "Tiago Silva", "tiago@example.com", "***.456.789-**", "ADVOGADO", "ADVOGADO", "PROFISSIONAL_EXTERNO", "CE", "Morada Nova", true, "prata", true, List.of("ROLE_ADVOGADO")));
        when(applicationService.capabilities()).thenReturn(new PjbFrontendCapabilitySummaryView("ADVOGADO", "PROFISSIONAL_EXTERNO", 2, false, false, List.of("DASHBOARD_PROCESSOS_PROPRIOS", "CALENDARIO_AUDIENCIAS")));
        when(applicationService.context(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(new PjbFrontendContextView("ADVOGADO", "PROFISSIONAL_EXTERNO", "Atuação Independente", true, true, false, true, "prata", true, false, 1, List.of("VERIFY_TRUSTED_DEVICE"), 1, List.of("ROLE_ADVOGADO"), "/advogado"));
        when(applicationService.menu(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(new PjbFrontendMenuItemView("office-mode", "Modo escritório", "/api/v1/frontend/app/office-mode", "processos", "prata", false, true)));
        when(applicationService.supportCatalogs()).thenReturn(new PjbFrontendSupportCatalogView(List.of("CIDADAO", "ADVOGADO"), List.of("CIVIL", "PENAL"), List.of("EM_ANDAMENTO"), List.of("COMUM"), List.of("PUBLIC", "AUTHENTICATED", "STEP_UP"), List.of("dashboard", "processos")));
        when(applicationService.ramoDireitoCatalog()).thenReturn(List.of(new PjbFrontendRamoDireitoCatalogEntry("01", "CIVIL", "Direito Civil", "Privado", "CIVEL", true, false, false), new PjbFrontendRamoDireitoCatalogEntry("17", "PROCESSUAL_CIVIL", "Direito Processual Civil", "Privado", "CIVEL", true, false, false)));
        when(applicationService.myOwnedOffices()).thenReturn(List.of(new PjbFrontendOwnedOfficeView(44L, "Escritorio Rocha & Silva", 10L, "Tiago Silva", true, true, List.of(), "HYBRID", true, true, 5, 5, 2, true)));
        when(applicationService.createOwnOffice(org.mockito.ArgumentMatchers.any())).thenReturn(officeMode);
        when(applicationService.ensurePersonalOffice()).thenReturn(officeMode);
        when(applicationService.officeWorkspaceSummary(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(new PjbFrontendOfficeWorkspaceSummaryView(44L, "Escritorio Rocha & Silva", 10L, "Tiago Silva", 77L, "Dr. Senior", true, true, true, "HYBRID", 1, 1, 5, 5, 2, true, true, List.of("CIVIL"), List.of("ASSINATURA_PATRONAL_OBRIGATORIA"), List.of("Membros online agora: 2."), List.of(new PjbFrontendOfficeTeamMemberView(10L, "Tiago Silva", "tiago@example.com", "12345/CE", "ADVOGADO_SENIOR", "Patrono", true, true, true, true, true, Instant.parse("2026-04-13T10:00:00Z"), 10, "FOUNDER")), List.of(new PjbFrontendOfficeTeamMemberView(10L, "Tiago Silva", "tiago@example.com", "12345/CE", "ADVOGADO_SENIOR", "Patrono", true, true, true, true, true, Instant.parse("2026-04-13T10:00:00Z"), 10, "FOUNDER"))));
        when(applicationService.officeMode(org.mockito.ArgumentMatchers.any())).thenReturn(officeMode);
        when(applicationService.updateOfficeMode(org.mockito.ArgumentMatchers.any())).thenReturn(officeMode);
        when(applicationService.clearOfficeMode()).thenReturn(new PjbFrontendOfficeModeView("PERSONAL", null, null, null, null, false, true, false, false, false, false, List.of(), List.of("Processos proprios em primeiro plano."), List.of("CIVIL", "PENAL"), true, null, null, null, false, 10L, "Tiago Silva"));

        when(applicationService.myIncomingOfficeInvites()).thenReturn(List.of(inviteView));
        when(applicationService.officeInvites(44L)).thenReturn(List.of(inviteView));
        when(applicationService.createOfficeInvite(org.mockito.ArgumentMatchers.any())).thenReturn(inviteView);
        when(applicationService.acceptOfficeInvite(org.mockito.ArgumentMatchers.eq(91L), org.mockito.ArgumentMatchers.any())).thenReturn(decisionResult);
        when(applicationService.confirmOfficeInviteActivation(org.mockito.ArgumentMatchers.eq(91L), org.mockito.ArgumentMatchers.any())).thenReturn(decisionResult);
        when(applicationService.rejectOfficeInvite(91L)).thenReturn(inviteView);
        when(applicationService.revokeOfficeInvite(91L)).thenReturn(inviteView);

        when(applicationService.myIncomingOfficeTransfers()).thenReturn(List.of(transferView));
        when(applicationService.officeTransfers(44L)).thenReturn(List.of(transferView));
        when(applicationService.previewOfficeTransfer(org.mockito.ArgumentMatchers.any())).thenReturn(transferPreviewView);
        when(applicationService.createOfficeTransfer(org.mockito.ArgumentMatchers.any())).thenReturn(transferView);
        when(applicationService.acceptOfficeTransfer(org.mockito.ArgumentMatchers.eq(501L), org.mockito.ArgumentMatchers.any())).thenReturn(transferView);
        when(applicationService.rejectOfficeTransfer(501L)).thenReturn(transferView);
        when(applicationService.officeWorkspaceProcesses(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(workspaceProcessPageView);
        when(applicationService.officeWorkspaceProcessAccess(org.mockito.ArgumentMatchers.eq(1001L), org.mockito.ArgumentMatchers.eq("PETICIONAR"), org.mockito.ArgumentMatchers.any())).thenReturn(workspaceProcessAccessView);
        when(applicationService.officeWorkspaceQueue(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(queuePageView);
        when(applicationService.previewOfficeGovernedDocumentBatch(org.mockito.ArgumentMatchers.eq(1001L), org.mockito.ArgumentMatchers.eq(java.util.UUID.fromString("11111111-1111-1111-1111-111111111111")), org.mockito.ArgumentMatchers.any())).thenReturn(documentBatchPreviewView);
        when(applicationService.linkOfficeGovernedDocumentBatch(org.mockito.ArgumentMatchers.eq(1001L), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(documentBatchLinkView);
        when(applicationService.submitOfficeGovernedProtocol(org.mockito.ArgumentMatchers.eq(1001L), org.mockito.ArgumentMatchers.eq(3001L), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(governedProtocolSubmissionView);
        when(applicationService.createOfficeGovernedUploadBatch(org.mockito.ArgumentMatchers.eq(1001L), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(uploadBatchView);
        when(applicationService.officeGovernedUploadBatch(org.mockito.ArgumentMatchers.eq(1001L), org.mockito.ArgumentMatchers.eq(java.util.UUID.fromString("22222222-2222-2222-2222-222222222222")), org.mockito.ArgumentMatchers.any())).thenReturn(uploadBatchView);
        when(applicationService.reserveOfficeGovernedUploadItem(org.mockito.ArgumentMatchers.eq(1001L), org.mockito.ArgumentMatchers.eq(java.util.UUID.fromString("22222222-2222-2222-2222-222222222222")), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(uploadReservationView);
        when(applicationService.directOfficeGovernedUpload(org.mockito.ArgumentMatchers.eq(1001L), org.mockito.ArgumentMatchers.eq(java.util.UUID.fromString("22222222-2222-2222-2222-222222222222")), org.mockito.ArgumentMatchers.eq(java.util.UUID.fromString("33333333-3333-3333-3333-333333333333")), org.mockito.ArgumentMatchers.eq("tkn"), org.mockito.ArgumentMatchers.any())).thenReturn(uploadIngressView);
        when(applicationService.finalizeOfficeGovernedUploadBatch(org.mockito.ArgumentMatchers.eq(1001L), org.mockito.ArgumentMatchers.eq(java.util.UUID.fromString("22222222-2222-2222-2222-222222222222")), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(uploadFinalizeView);
        when(applicationService.previewOfficeGovernedMultimediaWorkspace(org.mockito.ArgumentMatchers.eq(1001L), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(multimediaWorkspaceView);

        when(applicationService.bootstrap(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(new PjbFrontendAppBootstrapView(
                new PjbFrontendCurrentUserView(10L, "Tiago Silva", "tiago@example.com", "***.456.789-**", "ADVOGADO", "ADVOGADO", "PROFISSIONAL_EXTERNO", "CE", "Morada Nova", true, "prata", true, List.of("ROLE_ADVOGADO")),
                new PjbFrontendContextView("ADVOGADO", "PROFISSIONAL_EXTERNO", "Atuação Independente", true, true, false, true, "prata", true, false, 1, List.of("VERIFY_TRUSTED_DEVICE"), 1, List.of("ROLE_ADVOGADO"), "/advogado"),
                new PjbFrontendCapabilitySummaryView("ADVOGADO", "PROFISSIONAL_EXTERNO", 2, false, false, List.of("DASHBOARD_PROCESSOS_PROPRIOS", "CALENDARIO_AUDIENCIAS")),
                List.of(new PjbFrontendMenuItemView("office-mode", "Modo escritório", "/api/v1/frontend/app/office-mode", "processos", "prata", false, true)),
                new PjbFrontendSupportCatalogView(List.of("CIDADAO", "ADVOGADO"), List.of("CIVIL", "PENAL"), List.of("EM_ANDAMENTO"), List.of("COMUM"), List.of("PUBLIC", "AUTHENTICATED", "STEP_UP"), List.of("dashboard", "processos")),
                officeMode,
                new PjbFrontendOfficeWorkspaceSummaryView(44L, "Escritorio Rocha & Silva", 10L, "Tiago Silva", 77L, "Dr. Senior", true, true, true, "HYBRID", 1, 1, 5, 5, 2, true, true, List.of("CIVIL"), List.of("ASSINATURA_PATRONAL_OBRIGATORIA"), List.of("Membros online agora: 2."), List.of(new PjbFrontendOfficeTeamMemberView(10L, "Tiago Silva", "tiago@example.com", "12345/CE", "ADVOGADO_SENIOR", "Patrono", true, true, true, true, true, Instant.parse("2026-04-13T10:00:00Z"), 10, "FOUNDER")), List.of(new PjbFrontendOfficeTeamMemberView(10L, "Tiago Silva", "tiago@example.com", "12345/CE", "ADVOGADO_SENIOR", "Patrono", true, true, true, true, true, Instant.parse("2026-04-13T10:00:00Z"), 10, "FOUNDER"))),
                List.of("/api/v1/frontend/app/me", "/api/v1/frontend/app/office-mode")
        ));

        mockMvc.perform(get("/api/v1/frontend/app/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nome").value("Tiago Silva"));

        mockMvc.perform(get("/api/v1/frontend/app/support/catalogs/ramos-direito"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[1].name").value("PROCESSUAL_CIVIL"));

        mockMvc.perform(get("/api/v1/frontend/app/offices/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].officeName").value("Escritorio Rocha & Silva"));

        mockMvc.perform(post("/api/v1/frontend/app/offices")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(new FrontendOfficeWorkspaceCreateRequest("Escritorio Rocha & Silva", "HYBRID", true, true, true, java.util.Set.<com.tcc.pjb.backend.model.entity.enums.RamoDireito>of()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mode").value("HYBRID"));

        mockMvc.perform(post("/api/v1/frontend/app/offices/personal/ensure"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mode").value("HYBRID"));

        mockMvc.perform(get("/api/v1/frontend/app/offices/workspace/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.officeName").value("Escritorio Rocha & Silva"))
                .andExpect(jsonPath("$.data.onlineMembers").value(2));

        mockMvc.perform(get("/api/v1/frontend/app/office-mode"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mode").value("HYBRID"))
                .andExpect(jsonPath("$.data.activeEquipeNome").value("Escritorio Rocha & Silva"));

        mockMvc.perform(put("/api/v1/frontend/app/office-mode")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(new FrontendOfficeModeUpdateRequest(44L, "HYBRID", true, true))))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().stringValues("Set-Cookie", org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.containsString("PJB_OFFICE_MODE=HYBRID"))));

        mockMvc.perform(delete("/api/v1/frontend/app/office-mode"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().stringValues("Set-Cookie", org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.containsString("PJB_OFFICE_MODE="))));

        mockMvc.perform(get("/api/v1/frontend/app/offices/invites/incoming"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("PENDING"));

        mockMvc.perform(get("/api/v1/frontend/app/offices/44/invites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].equipeNome").value("Escritorio Rocha & Silva"));

        mockMvc.perform(post("/api/v1/frontend/app/offices/invites")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(new FrontendOfficeAffiliationInviteRequest(44L, "Maria Lima", "maria@email.com", null, "12345/CE", com.tcc.pjb.backend.model.entity.enums.PapelEquipe.ADVOGADO_JUNIOR, "Associada", false, java.util.Set.of(com.tcc.pjb.backend.model.entity.enums.RamoDireito.CIVIL), 8, 40, false, true, "HYBRID", 10))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.inviteId").value(91L));

        mockMvc.perform(post("/api/v1/frontend/app/offices/invites/91/accept")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(new FrontendOfficeAffiliationDecisionRequest(true, true, "HYBRID", true, "idem-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activated").value(true));

        mockMvc.perform(post("/api/v1/frontend/app/offices/invites/91/confirm-activation")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(new FrontendOfficeAffiliationFinalApprovalRequest(true, "idem-2", "Aprovado pelo patrono"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activated").value(true));

        mockMvc.perform(post("/api/v1/frontend/app/offices/invites/91/reject"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        mockMvc.perform(delete("/api/v1/frontend/app/offices/invites/91"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.inviteId").value(91L));

        mockMvc.perform(get("/api/v1/frontend/app/offices/transfers/incoming"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].transferId").value(501L));

        mockMvc.perform(get("/api/v1/frontend/app/offices/44/transfers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("PENDING_DESTINATION_ACCEPTANCE"));

        mockMvc.perform(post("/api/v1/frontend/app/offices/transfers/preview")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(new FrontendOfficeProcessTransferRequest(44L, 45L, 99L, List.of(1001L, 1002L), "Redistribuicao formal", "Carteira civel", "tx-prev", "preview-hash-501"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.previewHash").value("preview-hash-501"));

        mockMvc.perform(post("/api/v1/frontend/app/offices/transfers")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(new FrontendOfficeProcessTransferRequest(44L, 45L, 99L, List.of(1001L, 1002L), "Redistribuicao formal", "Carteira civel", "tx-1", "preview-hash-501"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.transferId").value(501L));

        mockMvc.perform(post("/api/v1/frontend/app/offices/transfers/501/accept")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(new FrontendOfficeProcessTransferDecisionRequest(true, "tx-2"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.transferId").value(501L));

        mockMvc.perform(post("/api/v1/frontend/app/offices/transfers/501/reject"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.transferId").value(501L));

        mockMvc.perform(post("/api/v1/frontend/app/offices/workspace/processes/query")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(new FrontendOfficeWorkspaceProcessQueryRequest(0, 20, null, com.tcc.pjb.backend.model.entity.enums.StatusProcesso.EM_ANDAMENTO, null, false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.returnedCount").value(1))
                .andExpect(jsonPath("$.data.items[0].numeroProcesso").value("0001"));

        mockMvc.perform(get("/api/v1/frontend/app/offices/workspace/processes/1001/access").param("action", "PETICIONAR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.allowed").value(true))
                .andExpect(jsonPath("$.data.effectiveSignerNome").value("Dr. Senior"));


        mockMvc.perform(get("/api/v1/frontend/app/offices/workspace/queue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].summary").value("JUNTADA_DOCUMENTAL"));

        mockMvc.perform(get("/api/v1/frontend/app/offices/workspace/processes/1001/document-batches/11111111-1111-1111-1111-111111111111/preview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.batchFingerprint").value("batch-fingerprint-1"));

        mockMvc.perform(post("/api/v1/frontend/app/offices/workspace/processes/1001/document-batches/link")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(new FrontendOfficeGovernedDocumentBatchLinkRequest(java.util.UUID.fromString("11111111-1111-1111-1111-111111111111"), null, null, null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_SIGNER"));

        mockMvc.perform(post("/api/v1/frontend/app/offices/workspace/processes/1001/protocol-packages/3001/submit")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(new FrontendOfficeGovernedProtocolSubmitRequest("integrity-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_SIGNER"));

        mockMvc.perform(post("/api/v1/frontend/app/offices/workspace/processes/1001/uploads/batches")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(new FrontendOfficeGovernedUploadBatchCreateRequest(3))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.batchId").value("22222222-2222-2222-2222-222222222222"));

        mockMvc.perform(get("/api/v1/frontend/app/offices/workspace/processes/1001/uploads/batches/22222222-2222-2222-2222-222222222222"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.batchFingerprint").value("upload-batch-fingerprint-1"));

        mockMvc.perform(post("/api/v1/frontend/app/offices/workspace/processes/1001/uploads/batches/22222222-2222-2222-2222-222222222222/items")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(new FrontendOfficeGovernedUploadReserveItemRequest("peticao.pdf", "application/pdf", 2048, "sha384-reserva", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itemStatus").value("RESERVED"));

        mockMvc.perform(put("/api/v1/frontend/app/offices/workspace/processes/1001/uploads/direct/22222222-2222-2222-2222-222222222222/33333333-3333-3333-3333-333333333333")
                        .contentType("application/octet-stream")
                        .param("token", "tkn")
                        .content("conteudo".getBytes()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itemStatus").value("UPLOADED"));

        mockMvc.perform(post("/api/v1/frontend/app/offices/workspace/processes/1001/uploads/batches/22222222-2222-2222-2222-222222222222/finalize")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(new FrontendOfficeGovernedUploadFinalizeRequest("upload-batch-fingerprint-3", "idem-upload-1", "client-upload-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.jobId").value("44444444-4444-4444-4444-444444444444"));

        mockMvc.perform(post("/api/v1/frontend/app/offices/workspace/processes/1001/multimedia/workspace")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(new FrontendOfficeGovernedMultimediaWorkspaceRequest("PETICIONAR", "INSTITUCIONAL", "PETICAO_INSTITUCIONAL", true, false, false, java.util.Map.of("anexos", java.util.List.of("peticao.pdf"))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nextAction").value("SUBMETER_PECA_INSTITUCIONAL"));

        mockMvc.perform(get("/api/v1/frontend/app/bootstrap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.officeMode.mode").value("HYBRID"))
                .andExpect(jsonPath("$.data.nextApiCalls[1]").value("/api/v1/frontend/app/office-mode"));
    }

    @Test
    void endpoint_deveProtocolarPeticaoGovernadaNoModoEscritorio() throws Exception {
        PjbFrontendOfficeGovernedPetitionView petitionView = new PjbFrontendOfficeGovernedPetitionView(
                901L,
                "OFFICE",
                44L,
                "PETICIONAR",
                "PENDING_SIGNER",
                5001L,
                7001L,
                null,
                null,
                true,
                true,
                77L,
                "Dr. Patrono",
                "12345/CE",
                "PATRONO_CERTIFICATE",
                false,
                null,
                null,
                java.util.Map.of(),
                java.util.List.of(),
                java.util.List.of("ASSINATURA_PATRONAL_OBRIGATORIA")
        );
        when(applicationService.submitOfficeGovernedPetition(org.mockito.ArgumentMatchers.eq(901L), org.mockito.ArgumentMatchers.any(FrontendOfficeGovernedPetitionRequest.class), org.mockito.ArgumentMatchers.any())).thenReturn(petitionView);

        mockMvc.perform(post("/api/v1/frontend/app/offices/workspace/processes/901/petitions")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new FrontendOfficeGovernedPetitionRequest("PETICAO_INTERMEDIARIA", "Conteudo da peticao", "Base legal"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.processoId").value(901))
                .andExpect(jsonPath("$.data.status").value("PENDING_SIGNER"))
                .andExpect(jsonPath("$.data.effectiveSignerNome").value("Dr. Patrono"))
                .andExpect(jsonPath("$.data.signatureMode").value("PATRONO_CERTIFICATE"));
    }


@Test
void endpoints_devemExporCockpitJuridicoInstitucionalEReadingMode() throws Exception {
    CalendarWorkspaceResponse calendarWorkspace = new CalendarWorkspaceResponse(
            java.time.LocalDate.parse("2026-04-13"),
            java.time.LocalDate.parse("2026-05-14"),
            new CalendarWorkspaceResponse.CalendarProfileDto("ADVOGADO", "Advogado", "PRAZOS", List.of("PRAZOS"), List.of("PRAZOS"), List.of(), true),
            List.of(new CalendarWorkspaceResponse.CalendarColorLegendDto("BLUE", "Prazo regular", "Prazo dentro da janela prevista")),
            List.of()
    );
    CalculoJudicialWorkspaceResponse calculatorWorkspace = new CalculoJudicialWorkspaceResponse(
            "VISAO_GERAL",
            "Calculadora judicial",
            "Workspace pronto para o advogado no escritório",
            CalculoJudicialSolicitantePerfil.ADVOGADO,
            List.of("VISAO_GERAL"),
            List.of(),
            List.of(new com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialWorkspaceCardResponse(
                    "TRABALHISTA_CLT",
                    "Trabalhista",
                    "Resumo",
                    "VISAO_GERAL",
                    List.of("ADVOGADO"),
                    List.of(),
                    List.of("ENTRADAS"),
                    List.of("MEMORIA_DE_CALCULO"),
                    java.util.Map.of("entryRoute", "/api/v1/processual/calculos/workspace"),
                    java.util.Map.of("accentColor", "ORANGE")
            )),
            List.of(),
            List.of(),
            java.util.Map.of("entryRoute", "/api/v1/processual/calculos/workspace"),
            java.time.Instant.parse("2026-04-13T10:00:00Z")
    );
    PjbFrontendOfficeProcessAccessView accessView = new PjbFrontendOfficeProcessAccessView(
            1001L,
            "0001-11.2026.8.06.0001",
            44L,
            "OFFICE",
            "READ",
            true,
            true,
            false,
            77L,
            "Dr. Senior",
            List.of(),
            List.of("ASSINATURA_PATRONAL_OBRIGATORIA")
    );
    PjbFrontendOfficeProcessReadingModeView readingModeView = new PjbFrontendOfficeProcessReadingModeView(
            1001L,
            "0001-11.2026.8.06.0001",
            "OFFICE",
            44L,
            "Escritorio Rocha & Silva",
            true,
            "BLUE",
            "BLUE",
            "BLUE",
            "GREEN",
            accessView,
            calendarWorkspace,
            List.of(new TimelineItemResponse(9L, Instant.parse("2026-04-13T10:00:00Z"), "INICIAL", "SANEAMENTO", "Movimentação de leitura", 10L, "Tiago", true, false, 5L, 0L, "ATIVO", 1L, null, null, false, null)),
            List.of("TIMELINE_PROCESSUAL", "CALENDARIO_DE_PRAZOS"),
            List.of("/api/v1/timeline/processo/1001"),
            List.of(),
            List.of("ASSINATURA_PATRONAL_OBRIGATORIA")
    );
    PjbFrontendOfficeWorkspaceLegalCockpitView cockpitView = new PjbFrontendOfficeWorkspaceLegalCockpitView(
            "OFFICE",
            44L,
            "Escritorio Rocha & Silva",
            null,
            null,
            List.of(new PjbFrontendOfficeWorkspaceProcessCardView(1001L, "0001-11.2026.8.06.0001", "CIVIL", "EM_ANDAMENTO", "PUBLICO", "BLUE", "BLUE", "BLUE", "GREEN", true, false, false, true, List.of(), List.of("ASSINATURA_PATRONAL_OBRIGATORIA"), "/api/v1/frontend/app/offices/workspace/processes/1001/reading-mode", "/api/v1/timeline/processo/1001", "/api/v1/calendar/workspace?processoId=1001", "/api/v1/processual/calculos/workspace", "/api/v1/processo/1001/prazo-real")),
            calendarWorkspace,
            calculatorWorkspace,
            readingModeView,
            List.of("CALCULADORA_JUDICIAL", "MOVIMENTACAO_MODO_LEITURA"),
            List.of("/api/v1/frontend/app/offices/workspace/legal-cockpit"),
            List.of(),
            List.of("ASSINATURA_PATRONAL_VINCULADA_AO_WORKSPACE")
    );

    when(applicationService.officeWorkspaceLegalCockpit(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(1001L))).thenReturn(cockpitView);
    when(applicationService.officeProcessReadingMode(org.mockito.ArgumentMatchers.eq(1001L), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(readingModeView);

    mockMvc.perform(get("/api/v1/frontend/app/offices/workspace/legal-cockpit").param("processoId", "1001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.officeMode").value("OFFICE"))
            .andExpect(jsonPath("$.data.highlightedProcesses[0].calculatorRoute").value("/api/v1/processual/calculos/workspace"))
            .andExpect(jsonPath("$.data.linkedModules[0]").value("CALCULADORA_JUDICIAL"));

    mockMvc.perform(get("/api/v1/frontend/app/offices/workspace/processes/1001/reading-mode"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.readOnly").value(true))
            .andExpect(jsonPath("$.data.access.allowed").value(true))
            .andExpect(jsonPath("$.data.timeline[0].descricao").value("Movimentação de leitura"));
}

@Test
void officeWorkspaceMainDashboard_deveExporPainelInstitucionalUnificado() throws Exception {
    PjbFrontendOfficeWorkspaceMainDashboardView dashboardView = new PjbFrontendOfficeWorkspaceMainDashboardView(
            Instant.parse("2026-04-13T12:00:00Z"),
            "OFFICE",
            44L,
            "Escritorio Rocha & Silva",
            new PjbFrontendOfficeWorkspaceSummaryView(
                    44L,
                    "Escritorio Rocha & Silva",
                    77L,
                    "Dr. Senior",
                    77L,
                    "Dr. Senior",
                    false,
                    true,
                    true,
                    "OFFICE",
                    1L,
                    2L,
                    8L,
                    8L,
                    3L,
                    true,
                    true,
                    List.of("CIVIL", "TRABALHISTA"),
                    List.of(),
                    List.of("Cockpit institucional ativo."),
                    List.of(new PjbFrontendOfficeTeamMemberView(77L, "Dr. Senior", "senior@example.com", "OAB/CE 1234", "ADVOGADO_SENIOR", "Patrono", true, true, false, true, true, Instant.parse("2026-04-13T11:59:00Z"), 10, "FOUNDER")),
                    List.of(new PjbFrontendOfficeTeamMemberView(77L, "Dr. Senior", "senior@example.com", "OAB/CE 1234", "ADVOGADO_SENIOR", "Patrono", true, true, false, true, true, Instant.parse("2026-04-13T11:59:00Z"), 10, "FOUNDER"))
            ),
            new PjbFrontendOfficeWorkspaceMainDashboardKpiView(8L, 3L, 24L, 2L, 1L, 4L, 2L, 3L, 5L, 1L),
            null,
            List.of(new PjbFrontendOfficeTeamMemberView(77L, "Dr. Senior", "senior@example.com", "OAB/CE 1234", "ADVOGADO_SENIOR", "Patrono", true, true, false, true, true, Instant.parse("2026-04-13T11:59:00Z"), 10, "FOUNDER")),
            List.of(new PjbFrontendOfficeQueueItemView(900L, 44L, 10L, 77L, "PETICIONAR", "ADV_PROCESS_OPERATION", "501", "PENDING", java.time.LocalDateTime.parse("2026-04-13T11:58:00"), null, null, null, "req-1", "hash-1", "Petição aguardando patrono")),
            List.of(new PjbFrontendOfficeProcessTransferView(600L, 44L, "Escritorio Rocha & Silva", 55L, "Equipe Norte", 91L, "Dra. Ana", "PENDING_DESTINATION_ACCEPTANCE", 3, 1, "Redistribuição", "PARCIAL", List.of(1001L, 1002L, 1003L), "Preview ok", true, false, Instant.parse("2026-04-13T11:00:00Z"), null, null)),
            List.of(new PjbFrontendOfficeCriticalDeadlineView(300L, 1001L, "0001-11.2026.8.06.0001", "Prazo recursal", Instant.parse("2026-04-14T12:00:00Z"), 24L, "BLUE", "BLUE", "/api/v1/frontend/app/offices/workspace/processes/1001/reading-mode", "/api/v1/calendar/workspace?processoId=1001")),
            List.of(new PjbFrontendOfficePendingPetitionView(501L, 1001L, "0001-11.2026.8.06.0001", "PETICIONAR", "PENDING_SIGNER", 900L, 10L, 77L, "Dr. Senior", "OAB/CE 1234", true, "BLUE", java.time.LocalDateTime.parse("2026-04-13T11:57:00"), "/api/v1/frontend/app/offices/workspace/processes/1001/reading-mode")),
            List.of("/api/v1/frontend/app/offices/workspace/main-dashboard"),
            List.of(),
            List.of("COCKPIT_INSTITUCIONAL_HABILITADO")
    );

    when(applicationService.officeWorkspaceMainDashboard(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(dashboardView);

    mockMvc.perform(get("/api/v1/frontend/app/offices/workspace/main-dashboard"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.officeMode").value("OFFICE"))
            .andExpect(jsonPath("$.data.kpis.onlineMembers").value(3))
            .andExpect(jsonPath("$.data.pendingQueueItems[0].actionType").value("PETICIONAR"))
            .andExpect(jsonPath("$.data.pendingTransfers[0].status").value("PENDING_DESTINATION_ACCEPTANCE"))
            .andExpect(jsonPath("$.data.pendingPetitions[0].signerNome").value("Dr. Senior"));
}

}
