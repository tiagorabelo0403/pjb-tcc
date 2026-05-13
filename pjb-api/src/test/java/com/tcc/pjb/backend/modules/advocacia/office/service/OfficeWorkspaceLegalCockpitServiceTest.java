package com.tcc.pjb.backend.modules.advocacia.office.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeMembershipView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeModeView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeProcessAccessView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeWorkspaceProcessPageView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeWorkspaceProcessView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeWorkspaceSummaryView;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.calendar.CalendarWorkspaceResponse;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialSolicitantePerfil;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialWorkspaceCardResponse;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialWorkspaceResponse;
import com.tcc.pjb.backend.model.dto.timeline.TimelineItemResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.calendar.UserCalendarWorkspaceService;
import com.tcc.pjb.backend.service.processual.calculo.CalculoJudicialWorkspaceService;
import com.tcc.pjb.backend.service.timeline.surface.TimelineSurfaceFacadeService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;

class OfficeWorkspaceLegalCockpitServiceTest {

    @Test
    void cockpit_deveLigarCalculadoraCalendarioCoresETimelineAoWorkspace() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        OfficeWorkspaceModeService officeWorkspaceModeService = mock(OfficeWorkspaceModeService.class);
        OfficeWorkspaceDashboardService officeWorkspaceDashboardService = mock(OfficeWorkspaceDashboardService.class);
        OfficeProcessWorkspaceScopeService officeProcessWorkspaceScopeService = mock(OfficeProcessWorkspaceScopeService.class);
        UserCalendarWorkspaceService userCalendarWorkspaceService = mock(UserCalendarWorkspaceService.class);
        CalculoJudicialWorkspaceService calculoJudicialWorkspaceService = mock(CalculoJudicialWorkspaceService.class);
        TimelineSurfaceFacadeService timelineSurfaceFacadeService = mock(TimelineSurfaceFacadeService.class);
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);

        Usuario usuario = new Usuario();
        usuario.setId(10L);
        usuario.setTipoUsuario(TipoUsuario.ADVOGADO);
        when(currentUserService.getRequired()).thenReturn(usuario);

        when(officeWorkspaceModeService.current(org.mockito.ArgumentMatchers.any())).thenReturn(new PjbFrontendOfficeModeView(
                "OFFICE", 44L, "Escritorio Rocha & Silva", 77L, "Dr. Senior", true, true, false, false, false, false,
                List.of(new PjbFrontendOfficeMembershipView(44L, "Escritorio Rocha & Silva", "ASSOCIADO", "Associado", 77L, "Dr. Senior", true, false, true, true, true, List.of("CIVIL"), false, 6, "ELEVADO", 8, true, 20)),
                List.of(), List.of("CIVIL"), false, 6, "ELEVADO", 8, true, 77L, "Dr. Senior"));
        when(officeWorkspaceDashboardService.currentSummary(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(
                new PjbFrontendOfficeWorkspaceSummaryView(44L, "Escritorio Rocha & Silva", 77L, "Dr. Senior", 77L, "Dr. Senior", false, true, true, "OFFICE", 1, 1, 3, 3, 2, true, true, List.of("CIVIL"), List.of(), List.of(), List.of(), List.of())
        );
        when(officeProcessWorkspaceScopeService.currentWorkspaceProcesses(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(
                new PjbFrontendOfficeWorkspaceProcessPageView("OFFICE", 44L, "Escritorio Rocha & Silva", false, false, List.of("CIVIL"), 6, 8, 0, 12, 1, 1, List.of(), List.of(),
                        List.of(new PjbFrontendOfficeWorkspaceProcessView(1001L, "0001-11.2026.8.06.0001", 44L, "Escritorio Rocha & Silva", 10L, "Tiago", "CIVIL", "PUBLICO", "EM_ANDAMENTO", true, true, false, false, true, List.of(), List.of("ASSINATURA_PATRONAL_OBRIGATORIA"))))
        );
        when(userCalendarWorkspaceService.workspace(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(
                new CalendarWorkspaceResponse(LocalDate.parse("2026-04-13"), LocalDate.parse("2026-05-14"), new CalendarWorkspaceResponse.CalendarProfileDto("ADVOGADO", "Advogado", "PRAZOS", List.of("PRAZOS"), List.of("PRAZOS"), List.of(), true), List.of(), List.of())
        );
        when(calculoJudicialWorkspaceService.workspace(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(CalculoJudicialSolicitantePerfil.ADVOGADO), org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.any())).thenReturn(
                new CalculoJudicialWorkspaceResponse(
                        "VISAO_GERAL",
                        "Calculadora judicial",
                        "Workspace calculado para o advogado no escritório",
                        CalculoJudicialSolicitantePerfil.ADVOGADO,
                        List.of("VISAO_GERAL"),
                        List.of(),
                        List.of(new CalculoJudicialWorkspaceCardResponse(
                                "TRABALHISTA_CLT",
                                "Trabalhista",
                                "Resumo",
                                "VISAO_GERAL",
                                List.of("ADVOGADO"),
                                List.of(),
                                List.of("ENTRADAS"),
                                List.of("MEMORIA_DE_CALCULO"),
                                Map.of("entryRoute", "/api/v1/processual/calculos/workspace"),
                                Map.of("accentColor", "ORANGE")
                        )),
                        List.of(),
                        List.of(),
                        Map.of("entryRoute", "/api/v1/processual/calculos/workspace"),
                        Instant.parse("2026-04-13T10:00:00Z")
                )
        );
        when(officeProcessWorkspaceScopeService.access(org.mockito.ArgumentMatchers.eq(1001L), org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.any())).thenReturn(
                new PjbFrontendOfficeProcessAccessView(1001L, "0001-11.2026.8.06.0001", 44L, "OFFICE", "READ", true, true, false, 77L, "Dr. Senior", List.of(), List.of("ASSINATURA_PATRONAL_OBRIGATORIA"))
        );
        Processo processo = new Processo();
        processo.setId(1001L);
        processo.setNumeroUnificado("0001-11.2026.8.06.0001");
        processo.setRamoDireito(RamoDireito.CIVIL);
        processo.setStatusProcesso(StatusProcesso.EM_ANDAMENTO);
        processo.setNivelSigilo(NivelSigilo.PUBLICO);
        when(processoRepository.findById(1001L)).thenReturn(Optional.of(processo));
        when(timelineSurfaceFacadeService.timeline(1001L)).thenReturn(List.of(new TimelineItemResponse(9L, Instant.parse("2026-04-13T10:00:00Z"), "INICIAL", "SANEAMENTO", "Movimentação de leitura", 10L, "Tiago", true, false, 5L, 0L, "ATIVO", 1L, null, null, false, null)));

        OfficeWorkspaceLegalCockpitService service = new OfficeWorkspaceLegalCockpitService(currentUserService, officeWorkspaceModeService, officeWorkspaceDashboardService, officeProcessWorkspaceScopeService, userCalendarWorkspaceService, calculoJudicialWorkspaceService, timelineSurfaceFacadeService, processoRepository, auditLedgerService);

        var cockpit = service.cockpit(new TestingAuthenticationToken("tiago", "n/a", "ROLE_ADVOGADO"), new MockHttpServletRequest(), LocalDate.parse("2026-04-13"), LocalDate.parse("2026-05-14"), 1001L);

        assertThat(cockpit.linkedModules()).contains("CALCULADORA_JUDICIAL", "MOVIMENTACAO_MODO_LEITURA");
        assertThat(cockpit.highlightedProcesses()).hasSize(1);
        assertThat(cockpit.highlightedProcesses().get(0).calculatorRoute()).isEqualTo("/api/v1/processual/calculos/workspace");
        assertThat(cockpit.selectedProcessReading().timeline()).hasSize(1);
        assertThat(cockpit.selectedProcessReading().readOnly()).isTrue();
    }
}
