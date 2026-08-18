package com.tcc.pjb.backend.modules.notificacoes.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

import com.tcc.pjb.backend.model.dto.calendar.CalendarNotificationEnvelope;
import com.tcc.pjb.backend.modules.notificacoes.api.NotificacaoPrazoCommand;
import com.tcc.pjb.backend.service.calendar.CalendarNotificationEventPublisher;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class CalendarPrazoNotificacaoAdapterTest {

    @Test
    void publicaEnvelopeDeCalendarioSemRepositoryDireto() {
        CalendarNotificationEventPublisher publisher = Mockito.mock(CalendarNotificationEventPublisher.class);
        CalendarPrazoNotificacaoAdapter adapter = new CalendarPrazoNotificacaoAdapter(publisher);

        var result = adapter.publicarAlertaPrazo(new NotificacaoPrazoCommand(
                10L,
                20L,
                "0001234-56.2026.8.06.0001",
                LocalDate.of(2026, 4, 1),
                LocalDateTime.of(2026, 3, 31, 9, 0),
                "Prazo processual calculado",
                "Vencimento forense: 2026-04-01.",
                "/processos/20/prazos",
                "ALTA",
                "PRAZOS",
                "PRAZO:fixture"
        ));

        ArgumentCaptor<CalendarNotificationEnvelope> captor = ArgumentCaptor.forClass(CalendarNotificationEnvelope.class);
        verify(publisher).publish(captor.capture());
        CalendarNotificationEnvelope envelope = captor.getValue();
        assertNotNull(envelope.notificationId());
        assertEquals(10L, envelope.usuarioId());
        assertEquals(20L, envelope.processoId());
        assertEquals("PRAZO", envelope.laneCode());
        assertEquals("PRAZO_PROCESSUAL", envelope.segmentCode());
        assertEquals("ALTA", envelope.urgency());
        assertEquals("AMBER", envelope.color());
        assertEquals("PRAZO:fixture", envelope.notificationKey());
        assertEquals("PUBLICADA", result.status());
    }
}
