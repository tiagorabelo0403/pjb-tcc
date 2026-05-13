package com.tcc.pjb.backend.modules.advocacia.office.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeModeView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeProcessTransferView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeWorkspaceLegalCockpitView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeWorkspaceProcessCardView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeWorkspaceProcessPageView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeWorkspaceSummaryView;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.advogado.AdvogadoDashboardDto;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.modules.advocacia.office.dto.OfficeQueueItemDto;
import com.tcc.pjb.backend.modules.advocacia.office.entity.AdvOfficeProcessOperation;
import com.tcc.pjb.backend.modules.advocacia.office.entity.OfficeSignatureQueueItem;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeActionType;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeQueueStatus;
import com.tcc.pjb.backend.modules.advocacia.office.repository.AdvOfficeProcessOperationRepository;
import com.tcc.pjb.backend.service.advogado.AdvogadoDashboardService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;

class OfficeWorkspaceMainDashboardServiceTest {

    @Test
    void dashboard_deveUnificarWorkspaceFilaTransferenciaEPeticoes() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        OfficeWorkspaceModeService officeWorkspaceModeService = mock(OfficeWorkspaceModeService.class);
        OfficeWorkspaceDashboardService officeWorkspaceDashboardService = mock(OfficeWorkspaceDashboardService.class);
        OfficeWorkspaceLegalCockpitService officeWorkspaceLegalCockpitService = mock(OfficeWorkspaceLegalCockpitService.class);
        OfficeSignatureQueueService officeSignatureQueueService = mock(OfficeSignatureQueueService.class);
        OfficeProcessTransferService officeProcessTransferService = mock(OfficeProcessTransferService.class);
        AdvogadoDashboardService advogadoDashboardService = mock(AdvogadoDashboardService.class);
        AdvOfficeProcessOperationRepository processOperationRepository = mock(AdvOfficeProcessOperationRepository.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);

        Usuario usuario = new Usuario();
        usuario.setId(10L);
        usuario.setNome("Tiago Silva");
        when(currentUserService.getRequired()).thenReturn(usuario);
        when(officeWorkspaceModeService.current(any())).thenReturn(new PjbFrontendOfficeModeView(
                "OFFICE", 44L, "Escritorio Rocha & Silva", 77L, "Dr. Senior", true, true, true, false, false, false,
                List.of(), List.of(), List.of("CIVIL"), true, 5, "ALTO", 8, true, 77L, "Dr. Senior"
        ));
        when(officeWorkspaceDashboardService.currentSummary(any(), anyLong())).thenReturn(new PjbFrontendOfficeWorkspaceSummaryView(
                44L, "Escritorio Rocha & Silva", 77L, "Dr. Senior", 77L, "Dr. Senior", false, true, true,
                "OFFICE", 1L, 2L, 6L, 6L, 2L, true, true, List.of("CIVIL"), List.of(), List.of("Cockpit ativo."), List.of(), List.of()
        ));
        when(officeWorkspaceLegalCockpitService.cockpit(any(), any(), any(), any(), any())).thenReturn(new PjbFrontendOfficeWorkspaceLegalCockpitView(
                "OFFICE",
                44L,
                "Escritorio Rocha & Silva",
                null,
                new PjbFrontendOfficeWorkspaceProcessPageView("OFFICE", 44L, "Escritorio Rocha & Silva", false, false, List.of("CIVIL"), 5, 8, 0, 12, 18L, 1, List.of(), List.of(), List.of()),
                List.of(new PjbFrontendOfficeWorkspaceProcessCardView(1001L, "0001-11.2026.8.06.0001", "CIVIL", "EM_ANDAMENTO", "PUBLICO", "BLUE", "BLUE", "BLUE", "GREEN", true, false, false, true, List.of(), List.of(), "/api/v1/frontend/app/offices/workspace/processes/1001/reading-mode", "/api/v1/timeline/processo/1001", "/api/v1/calendar/workspace?processoId=1001", "/api/v1/processual/calculos/workspace", "/api/v1/processo/1001/prazo-real")),
                null,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of()
        ));
        when(officeSignatureQueueService.listarPorSigner(anyLong(), any(), any(PageRequest.class))).thenReturn(new PageImpl<>(List.of(
                OfficeQueueItemDto.builder().id(900L).equipeId(44L).executorUserId(10L).signerUserId(77L).actionType(OfficeActionType.PETICIONAR).resourceType("ADV_PROCESS_OPERATION").resourceId("501").status(OfficeQueueStatus.PENDING).createdAt(LocalDateTime.parse("2026-04-13T11:58:00")).summary("Petição aguardando patrono").build()
        )));
        when(officeProcessTransferService.officeTransfers(44L)).thenReturn(List.of(
                new PjbFrontendOfficeProcessTransferView(600L, 44L, "Escritorio Rocha & Silva", 55L, "Equipe Norte", 91L, "Dra. Ana", "PENDING_DESTINATION_ACCEPTANCE", 3, 1, "Redistribuição", "PARCIAL", List.of(1001L), "Preview ok", true, false, Instant.parse("2026-04-13T11:00:00Z"), null, null)
        ));
        when(advogadoDashboardService.summary(21, 12)).thenReturn(AdvogadoDashboardDto.SummaryResponse.builder()
                .generatedAt(Instant.parse("2026-04-13T12:00:00Z"))
                .agendaProxima(List.of(AdvogadoDashboardDto.AgendaEventLite.builder().id(1L).titulo("Audiencia").build()))
                .prazosCriticos(List.of(AdvogadoDashboardDto.WorkItemLite.builder().id(300L).processoId(1001L).processoNumero("0001-11.2026.8.06.0001").titulo("Prazo recursal").dueAt(Instant.now().plusSeconds(86400)).build()))
                .build());

        Processo processo = new Processo();
        processo.setId(1001L);
        processo.setNumeroProcesso("0001-11.2026.8.06.0001");
        AdvOfficeProcessOperation operation = new AdvOfficeProcessOperation();
        operation.setId(501L);
        operation.setActionType(OfficeActionType.PETICIONAR);
        operation.setStatus("PENDING_SIGNER");
        operation.setProcesso(processo);
        operation.setExecutor(usuario);
        Usuario signer = new Usuario();
        signer.setId(77L);
        operation.setSigner(signer);
        operation.setSignerNameSnapshot("Dr. Senior");
        operation.setSignerRegistrationSnapshot("OAB/CE 1234");
        OfficeSignatureQueueItem queueItem = new OfficeSignatureQueueItem();
        queueItem.setId(900L);
        operation.setQueueItem(queueItem);
        operation.setCreatedAt(LocalDateTime.parse("2026-04-13T11:57:00"));
        when(processOperationRepository.findDashboardPending(anyLong(), anyLong(), any(), any(), any(PageRequest.class))).thenReturn(List.of(operation));
        when(processOperationRepository.countDashboardPending(anyLong(), anyLong(), any(), any())).thenReturn(1L);

        OfficeWorkspaceMainDashboardService service = new OfficeWorkspaceMainDashboardService(
                currentUserService,
                officeWorkspaceModeService,
                officeWorkspaceDashboardService,
                officeWorkspaceLegalCockpitService,
                officeSignatureQueueService,
                officeProcessTransferService,
                advogadoDashboardService,
                processOperationRepository,
                auditLedgerService
        );

        var result = service.dashboard(new TestingAuthenticationToken("tiago", "n/a"), new MockHttpServletRequest(), null, null);

        assertThat(result.officeMode()).isEqualTo("OFFICE");
        assertThat(result.kpis().onlineMembers()).isEqualTo(2L);
        assertThat(result.pendingQueueItems()).hasSize(1);
        assertThat(result.pendingTransfers()).hasSize(1);
        assertThat(result.pendingPetitions()).hasSize(1);
        assertThat(result.criticalDeadlines()).hasSize(1);
        assertThat(result.quickRoutes()).contains("/api/v1/frontend/app/offices/workspace/main-dashboard");
    }
}
