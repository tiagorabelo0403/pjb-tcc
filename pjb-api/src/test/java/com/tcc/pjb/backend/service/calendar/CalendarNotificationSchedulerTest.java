package com.tcc.pjb.backend.service.calendar;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.dto.calendar.CalendarNotificationEnvelope;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.modules.notificacoes.api.NotificacaoPrazoCommand;
import com.tcc.pjb.backend.modules.notificacoes.api.NotificacaoPrazoDispatchResult;
import com.tcc.pjb.backend.modules.notificacoes.application.NotificacaoPrazoApplicationService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

class CalendarNotificationSchedulerTest {

    @Test
    void roteiaAlertaDePrazoParaFronteiraModularEMantemAgendaNoPublisherLegado() {
        UsuarioRepository usuarioRepository = Mockito.mock(UsuarioRepository.class);
        UserCalendarNotificationPreviewService previewService = Mockito.mock(UserCalendarNotificationPreviewService.class);
        CalendarNotificationEventPublisher eventPublisher = Mockito.mock(CalendarNotificationEventPublisher.class);
        NotificacaoPrazoApplicationService prazoApplicationService = Mockito.mock(NotificacaoPrazoApplicationService.class);
        Usuario usuario = usuarioAtivo();
        CalendarNotificationEnvelope prazo = envelope("PRAZOS", "PRAZO_PROCESSUAL", "ALTA", "PRAZO:100");
        CalendarNotificationEnvelope agenda = envelope("AGENDA_PROCESSUAL", "AUDIENCIA", "NORMAL", "AGENDA:100");

        when(usuarioRepository.findByAtivoTrue(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(usuario)));
        when(previewService.dueNotificationsForUser(eq(usuario), any(), any(), isNull(), any())).thenReturn(List.of(prazo, agenda));
        when(prazoApplicationService.publicarAlertaPrazo(any())).thenReturn(new NotificacaoPrazoDispatchResult(true, "PUBLICADA", "PRAZO:100", "ALTA"));

        CalendarNotificationScheduler scheduler = new CalendarNotificationScheduler(usuarioRepository, previewService, eventPublisher, prazoApplicationService);
        scheduler.scanAndDispatch();

        ArgumentCaptor<NotificacaoPrazoCommand> captor = ArgumentCaptor.forClass(NotificacaoPrazoCommand.class);
        verify(prazoApplicationService).publicarAlertaPrazo(captor.capture());
        verify(eventPublisher).publish(agenda);
        verify(eventPublisher, never()).publish(prazo);
        NotificacaoPrazoCommand command = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(10L, command.usuarioId());
        org.junit.jupiter.api.Assertions.assertEquals(20L, command.processoId());
        org.junit.jupiter.api.Assertions.assertEquals("ALTA", command.prioridade());
        org.junit.jupiter.api.Assertions.assertEquals("CALENDAR", command.origemModulo());
        org.junit.jupiter.api.Assertions.assertEquals("PRAZO:100", command.notificationKey());
    }

    @Test
    void usaPublisherLegadoQuandoFronteiraModularRecusaAlertaDePrazo() {
        UsuarioRepository usuarioRepository = Mockito.mock(UsuarioRepository.class);
        UserCalendarNotificationPreviewService previewService = Mockito.mock(UserCalendarNotificationPreviewService.class);
        CalendarNotificationEventPublisher eventPublisher = Mockito.mock(CalendarNotificationEventPublisher.class);
        NotificacaoPrazoApplicationService prazoApplicationService = Mockito.mock(NotificacaoPrazoApplicationService.class);
        Usuario usuario = usuarioAtivo();
        CalendarNotificationEnvelope prazo = envelope("PRAZOS", "PRAZO_PROCESSUAL", "ALTA", "PRAZO:100");

        when(usuarioRepository.findByAtivoTrue(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(usuario)));
        when(previewService.dueNotificationsForUser(eq(usuario), any(), any(), isNull(), any())).thenReturn(List.of(prazo));
        when(prazoApplicationService.publicarAlertaPrazo(any())).thenThrow(new IllegalArgumentException("rota modular indisponivel"));

        CalendarNotificationScheduler scheduler = new CalendarNotificationScheduler(usuarioRepository, previewService, eventPublisher, prazoApplicationService);
        scheduler.scanAndDispatch();

        verify(eventPublisher).publish(prazo);
    }

    private Usuario usuarioAtivo() {
        Usuario usuario = new Usuario();
        usuario.setId(10L);
        usuario.setAtivo(true);
        return usuario;
    }

    private CalendarNotificationEnvelope envelope(String laneCode,
                                                  String segmentCode,
                                                  String urgency,
                                                  String key) {
        return new CalendarNotificationEnvelope(
                UUID.nameUUIDFromBytes(key.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                10L,
                20L,
                "0001234-56.2026.8.06.0001",
                "PROCESSUAL",
                laneCode,
                segmentCode,
                "STAGE",
                urgency,
                "AMBER",
                "Prazo processual calculado",
                "Vencimento forense confirmado.",
                "/processos/20",
                "USUARIO",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now(),
                Instant.now(),
                key
        );
    }
}
