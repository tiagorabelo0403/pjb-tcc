package com.tcc.pjb.backend.service.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.dto.calendar.CalendarInstitutionalFocusResponse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.calendar.CalendarWorkspaceEventDto;
import com.tcc.pjb.backend.model.dto.calendar.CalendarWorkspaceResponse;
import com.tcc.pjb.backend.model.dto.calendar.UserCalendarPreferenceResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalendarInstitutionalBridgeServiceTest {

    @Test
    void buildsCardsByWindowAndPriority() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        UserCalendarWorkspaceService workspaceService = mock(UserCalendarWorkspaceService.class);
        UserCalendarPreferenceService preferenceService = mock(UserCalendarPreferenceService.class);
        CalendarInstitutionalScopeService scopeService = mock(CalendarInstitutionalScopeService.class);
        CalendarInstitutionalContextService contextService = mock(CalendarInstitutionalContextService.class);
        CalendarInstitutionalBridgeService service = new CalendarInstitutionalBridgeService(
                currentUserService,
                workspaceService,
                preferenceService,
                scopeService,
                contextService,
                new CalendarEventAttentionPolicyService()
        );

        Usuario usuario = new Usuario();
        usuario.setId(99L);
        LocalDate from = LocalDate.now();
        LocalDate to = from.plusDays(7);
        when(preferenceService.currentOrDefault(usuario)).thenReturn(new UserCalendarPreferenceResponse(
                99L,
                List.of("AGENDA_PROCESSUAL", "PRAZOS"),
                List.of("AGENDA_PROCESSUAL"),
                List.of(),
                "MONTH",
                false,
                true,
                true,
                "INSTITUCIONAL",
                null,
                "CENTRAL_MANDADOS",
                "SMART",
                List.of("AGENDA_PROCESSUAL"),
                List.of(),
                List.of(),
                java.time.Instant.now()
        ));
        when(scopeService.availableScopes(usuario, false, true, null)).thenReturn(List.of(
                new CalendarInstitutionalScopeService.ScopeOption("INSTITUCIONAL", "Agenda institucional", "OFICIAL/CE", "INSTITUCIONAL")
        ));
        when(scopeService.normalizeActiveScope("INSTITUCIONAL", List.of(
                new CalendarInstitutionalScopeService.ScopeOption("INSTITUCIONAL", "Agenda institucional", "OFICIAL/CE", "INSTITUCIONAL")
        ), false, true)).thenReturn("INSTITUCIONAL");
        when(contextService.availableContexts(usuario, "INSTITUCIONAL", null, null)).thenReturn(List.of(
                new CalendarInstitutionalContextService.InstitutionalContextOption("CENTRAL_MANDADOS", "Central de mandados", "Mandados, diligências e certidões", "OPERACIONAL")
        ));
        when(contextService.normalizeActiveContext("CENTRAL_MANDADOS", List.of(
                new CalendarInstitutionalContextService.InstitutionalContextOption("CENTRAL_MANDADOS", "Central de mandados", "Mandados, diligências e certidões", "OPERACIONAL")
        ), "INSTITUCIONAL")).thenReturn("CENTRAL_MANDADOS");
        LocalDateTime tentativaAt = from.atTime(10, 0);
        LocalDateTime retornoAt = from.atTime(16, 0);
        when(workspaceService.workspaceForUser(usuario, from, to, null)).thenReturn(new CalendarWorkspaceResponse(
                from,
                to,
                new CalendarWorkspaceResponse.CalendarProfileDto("OFICIAL_JUSTICA", "Oficial de Justiça", "AGENDA_PROCESSUAL", List.of("AGENDA_PROCESSUAL"), List.of("AGENDA_PROCESSUAL"), List.of(), false),
                List.of(),
                List.of(
                        new CalendarWorkspaceResponse.CalendarLaneDto(
                                "AGENDA_PROCESSUAL",
                                "Agenda processual",
                                "RED",
                                true,
                                true,
                                2,
                                List.of(),
                                List.of(new CalendarWorkspaceResponse.CalendarDayDto(from, List.of(
                                        new CalendarWorkspaceEventDto("AGENDA_PROCESSUAL", "AGENDA_MANDADOS", "Mandados", "MANDADO_TENTATIVA", 1L, 10L, "0001", "Tentativa 1", "Central", tentativaAt, "RED", false, "/api/v1/oficial/10", null, "OFICIAL"),
                                        new CalendarWorkspaceEventDto("AGENDA_PROCESSUAL", "AGENDA_MANDADOS", "Mandados", "MANDADO_RETORNO", 2L, 10L, "0001", "Retorno", "Central", retornoAt, "AMBER", false, "/api/v1/oficial/10", null, "OFICIAL")
                                )))
                        )
                )
        ));

        var response = service.bridgeForUser(usuario, from, to, null);

        assertEquals("CENTRAL_MANDADOS", response.activeInstitutionContextCode());
        assertEquals(1, response.cards().size());
        assertEquals("CRITICA", response.cards().get(0).priorityCode());
        assertEquals(2, response.cards().get(0).totalEvents());
        assertEquals("MANDADO_TENTATIVA", response.cards().get(0).presentationCode());
        assertTrue(response.cards().get(0).attentionScore() >= 80);
        assertTrue(response.cards().get(0).highlights().stream().anyMatch(item -> item.contains("0001")));

        CalendarInstitutionalFocusResponse focus = service.focus(response);
        assertEquals("MANDADOS", focus.focusSlices().get(0).sliceCode());
        assertEquals(2, focus.focusSlices().get(0).totalEvents());
        assertTrue(focus.focusSlices().get(0).attentionScore() >= 80);
        assertEquals(2, focus.focusSlices().get(0).detailBuckets().size());
        assertTrue(focus.focusSlices().get(0).detailBuckets().stream().anyMatch(item -> item.detailCode().equals("TENTATIVAS") && item.totalEvents() == 1));
        assertTrue(focus.focusSlices().get(0).detailBuckets().stream().anyMatch(item -> item.detailCode().equals("RETORNO") && item.totalEvents() == 1));
        assertTrue(focus.milestones().stream().anyMatch(item -> item.title().contains("Mandados")));
    }
}
