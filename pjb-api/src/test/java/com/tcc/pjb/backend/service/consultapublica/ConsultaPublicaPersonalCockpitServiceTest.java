package com.tcc.pjb.backend.service.consultapublica;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.consultapublica.ConsultaPublicaPersonalWorkspaceHubDto;
import com.tcc.pjb.backend.model.dto.consultapublica.ConsultaPublicaPersonalWorkspaceSummaryDto;
import com.tcc.pjb.backend.model.dto.consultapublica.ConsultaPublicaWorkspaceResponse;
import com.tcc.pjb.backend.model.dto.consultapublica.ConsultaPublicaWorkspaceRoutesDto;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.repository.processo.ProcessoNoteRepository;
import com.tcc.pjb.backend.model.repository.workspace.WorkspaceProcessoEtiquetaRepository;
import com.tcc.pjb.backend.service.calendar.UserCalendarPanelService;
import com.tcc.pjb.backend.service.calendar.UserCalendarWorkspaceService;
import com.tcc.pjb.backend.service.cidadao.CidadaoProcessoOverviewService;
import com.tcc.pjb.backend.service.prazo.CongestionScoreService;
import com.tcc.pjb.backend.service.processual.calculo.CalculoJudicialWorkspaceService;
import com.tcc.pjb.backend.service.security.access.PersonalProcessAccessGuardService;
import com.tcc.pjb.backend.service.timeline.surface.TimelineSurfaceFacadeService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

class ConsultaPublicaPersonalCockpitServiceTest {

    @Test
    void cockpitWithoutPersonalProcessesStillReturnsIntegratedCapabilities() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        PersonalProcessAccessGuardService guardService = mock(PersonalProcessAccessGuardService.class);
        ConsultaPublicaWorkspaceService workspaceService = mock(ConsultaPublicaWorkspaceService.class);
        UserCalendarPanelService panelService = mock(UserCalendarPanelService.class);

        Usuario usuario = Usuario.builder().id(99L).cpf("04106184389").build();
        when(currentUserService.getRequired()).thenReturn(usuario);
        when(guardService.requireOwnProcessAccess("CONSULTA_PUBLICA_PERSONAL_COCKPIT")).thenReturn(new PersonalProcessAccessGuardService.PersonalProcessAccessEnvelope(
                true,
                "CONSULTA_PUBLICA_PERSONAL_COCKPIT",
                "GOVBR_PESSOAL",
                false,
                true,
                true,
                true,
                List.of("GOVBR_SESSION"),
                List.of(),
                List.of(),
                "ok"
        ));
        when(workspaceService.workspace()).thenReturn(new ConsultaPublicaWorkspaceResponse(
                "etag",
                java.time.LocalDateTime.now(),
                "PERSONAL_AND_PUBLIC",
                "headline",
                "summary",
                null,
                new ConsultaPublicaWorkspaceRoutesDto(
                        "/api/v1/public/consultas-publicas/workspace",
                        "/api/v1/public/consultas-publicas/search",
                        "/api/v1/public/consultas-publicas/processos/{numero}",
                        "/api/v1/processos/pessoais/meus-processos",
                        "/api/v1/processos/pessoais/cockpit",
                        "/api/v1/processos/pessoais/{processoId}/overview",
                        "/api/v1/processos/{processoId}/prazo-real?tipoAto=ATO_PROCESSUAL",
                        "/api/v1/public/consultas-publicas/pages/{pageId}",
                        "/api/v1/public/processos-pessoas/candidatos",
                        "/api/v1/public/processos-pessoas/candidatos/{identityKey}/processos",
                        "/api/v1/public/processos-pessoas/cpf/{cpf}/processos",
                        "/api/v1/calendar/workspace?from={from}&to={to}&processoId={processoId}",
                        "/api/v1/calendar/panel?from={from}&to={to}&processoId={processoId}",
                        "/api/v1/processos/{processoId}/prazo-real?tipoAto=ATO_PROCESSUAL",
                        "/api/v1/processual/calculos/workspace",
                        "/api/v1/processual/calculos/workspace/{dominio}/ajuda",
                        "/api/v1/chat/processo/{processoId}",
                        "/api/v1/processos/{processoId}/notes",
                        "/api/v1/workspace/etiquetas",
                        "/api/v1/workspace/processos/{processoId}/etiquetas",
                        "/api/v1/professional/forensic-panel/workspace",
                        "/api/v1/professional/forensic-panel/process-search",
                        "/api/v1/professional/forensic-panel/institutional-overview",
                        "/api/v1/frontend/app/professional/workspace/organizational-executive-dashboard",
                        "/api/v1/professional/forensic-panel/client-360",
                        "/api/v1/professional/forensic-panel/processos/{numero}",
                        "/api/v1/professional/access-grants/workspace",
                        "/api/v1/professional/access-grants/processos/{numero}/timeline",
                        "/api/v1/professional/access-grants/governance-dashboard",
                        "/api/v1/professional/access-grants/batch-requests",
                        "/api/v1/professional/access-grants/operational-dashboard",
                        "/api/v1/professional/access-grants/templates",
                        "/api/v1/professional/access-grants/template-batch-requests"
                ),
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                new ConsultaPublicaPersonalWorkspaceHubDto(
                        "headline",
                        "summary",
                        new ConsultaPublicaPersonalWorkspaceSummaryDto(0, 0, 0, 0, 0, 0),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of()
                ),
                List.of(),
                List.of()
        ));
        when(panelService.panelForUser(usuario, LocalDate.of(2026, 4, 13), LocalDate.of(2026, 5, 14), null))
                .thenReturn(new com.tcc.pjb.backend.model.dto.calendar.CalendarPanelResponse(
                        java.time.Instant.now(),
                        LocalDate.of(2026, 4, 13),
                        LocalDate.of(2026, 5, 14),
                        null,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        null,
                        null,
                        null
                ));

        ConsultaPublicaPersonalCockpitService service = new ConsultaPublicaPersonalCockpitService(
                currentUserService,
                guardService,
                workspaceService,
                mock(CidadaoProcessoOverviewService.class),
                mock(UserCalendarWorkspaceService.class),
                panelService,
                mock(CongestionScoreService.class),
                mock(TimelineSurfaceFacadeService.class),
                mock(WorkspaceProcessoEtiquetaRepository.class),
                mock(ProcessoNoteRepository.class),
                mock(CalculoJudicialWorkspaceService.class)
        );

        var response = service.cockpit(mock(Authentication.class), null, LocalDate.of(2026, 4, 13), LocalDate.of(2026, 5, 14));

        assertThat(response.mode()).isEqualTo("PERSONAL_COCKPIT");
        assertThat(response.accessMode()).isEqualTo("GOVBR_PESSOAL");
        assertThat(response.spotlight()).isNull();
        assertThat(response.quickActions()).extracting("code").contains("COCKPIT");
        assertThat(response.capabilityFlags()).contains("CALENDAR_INTEGRATED", "JUDICIAL_CALCULATOR_LINKED", "AI_CONTEXTUAL_ASSIST_LINKED");
    }
}
