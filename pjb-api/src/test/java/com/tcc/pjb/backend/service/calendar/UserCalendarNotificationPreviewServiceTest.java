package com.tcc.pjb.backend.service.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyIterable;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.calendar.CalendarWorkspaceEventDto;
import com.tcc.pjb.backend.model.dto.calendar.CalendarWorkspaceResponse;
import com.tcc.pjb.backend.model.dto.calendar.UserCalendarPreferenceResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.repository.NotificationHistoryRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class UserCalendarNotificationPreviewServiceTest {

    @Test
    void prioritizesPrazoAndAgendaNearTerm() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        UserCalendarWorkspaceService workspaceService = mock(UserCalendarWorkspaceService.class);
        NotificationHistoryRepository historyRepository = mock(NotificationHistoryRepository.class);
        UserCalendarPreferenceService preferenceService = mock(UserCalendarPreferenceService.class);
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        CalendarNotificationCadencePolicyService cadencePolicyService = new CalendarNotificationCadencePolicyService();
        UserCalendarNotificationPreviewService service = new UserCalendarNotificationPreviewService(
                currentUserService,
                workspaceService,
                historyRepository,
                preferenceService,
                processoRepository,
                cadencePolicyService,
                new CalendarEventAttentionPolicyService()
        );

        Usuario usuario = new Usuario();
        usuario.setId(10L);
        when(historyRepository.countByUsuarioIdAndLidoEmIsNull(10L)).thenReturn(3L);
        when(preferenceService.currentOrDefault(usuario)).thenReturn(new UserCalendarPreferenceResponse(
                10L,
                List.of("PRAZOS", "AGENDA_PROCESSUAL"),
                List.of("PRAZOS"),
                List.of(),
                "MONTH",
                true,
                false,
                true,
                "PROCESSUAL",
                null,
                "GERAL",
                "STRICT",
                List.of("PRAZOS", "AGENDA_PROCESSUAL"),
                List.of(),
                List.of(),
                java.time.Instant.now()
        ));

        LocalDate today = LocalDate.now();
        CalendarWorkspaceResponse workspace = new CalendarWorkspaceResponse(
                today,
                today.plusDays(7),
                new CalendarWorkspaceResponse.CalendarProfileDto("CIDADAO", "Cidadão", "AGENDA_PROCESSUAL", List.of("PRAZOS", "AGENDA_PROCESSUAL"), List.of("PRAZOS"), List.of(), true),
                List.of(),
                List.of(
                        new CalendarWorkspaceResponse.CalendarLaneDto(
                                "PRAZOS",
                                "Prazos",
                                "AMBER",
                                true,
                                true,
                                1,
                                List.of(),
                                List.of(new CalendarWorkspaceResponse.CalendarDayDto(today.plusDays(1), List.of(new CalendarWorkspaceEventDto(
                                        "PRAZOS", "PRAZO_RECURSAL_CPC", "Recursal", "PRAZO", 1L, 100L, "0001", "Contrarrazões", "CPC recursal", LocalDateTime.now().plusHours(20), "AMBER", false, "/api/v1/processos/100", "CPC recursal", "CIDADAO"
                                ))))
                        ),
                        new CalendarWorkspaceResponse.CalendarLaneDto(
                                "AGENDA_PROCESSUAL",
                                "Agenda",
                                "RED",
                                true,
                                false,
                                1,
                                List.of(),
                                List.of(new CalendarWorkspaceResponse.CalendarDayDto(today, List.of(new CalendarWorkspaceEventDto(
                                        "AGENDA_PROCESSUAL", "AGENDA_AUDIENCIAS", "Audiências", "AUDIENCIA", 2L, 101L, "0002", "Audiência de instrução", "Presencial", LocalDateTime.now().plusHours(3), "RED", false, "/api/v1/processos/101", null, "CIDADAO"
                                ))))
                        )
                )
        );

        Processo p100 = Processo.builder().id(100L).ramoDireito(RamoDireito.CIVIL).build();
        Processo p101 = Processo.builder().id(101L).ramoDireito(RamoDireito.CIVIL).build();
        when(workspaceService.workspaceForUser(usuario, today, today.plusDays(7), null)).thenReturn(workspace);
        when(processoRepository.findAllById(anyIterable())).thenReturn(List.of(p100, p101));

        var preview = service.previewForUser(usuario, today, today.plusDays(7), null);

        assertEquals(2, preview.totalPending());
        assertEquals(2, preview.criticalPending());
        assertEquals(3L, preview.unreadInbox());
        assertTrue(preview.items().stream().anyMatch(item -> item.stageCode().startsWith("RECURSAL") || item.stageCode().startsWith("PRAZO")));
        assertTrue(preview.items().stream().anyMatch(item -> item.stageCode().startsWith("AUDIENCIA") || item.stageCode().startsWith("SESSAO")));
        assertTrue(preview.items().stream().allMatch(item -> item.windowLabel() != null && item.cadenceMode() != null));
        assertTrue(preview.items().stream().allMatch(item -> item.presentationCode() != null && item.iconCode() != null));
        assertTrue(preview.items().stream().allMatch(item -> item.attentionScore() > 0));
    }
}
