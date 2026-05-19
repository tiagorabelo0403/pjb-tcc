package com.tcc.pjb.backend.modules.notificacoes.infrastructure;

import com.tcc.pjb.backend.model.dto.calendar.CalendarNotificationEnvelope;
import com.tcc.pjb.backend.modules.notificacoes.api.NotificacaoPrazoCommand;
import com.tcc.pjb.backend.modules.notificacoes.api.NotificacaoPrazoDispatchResult;
import com.tcc.pjb.backend.modules.notificacoes.api.NotificacaoPrazoPort;
import com.tcc.pjb.backend.service.calendar.CalendarNotificationEventPublisher;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CalendarPrazoNotificacaoAdapter implements NotificacaoPrazoPort {

    private final CalendarNotificationEventPublisher eventPublisher;

    public CalendarPrazoNotificacaoAdapter(CalendarNotificationEventPublisher eventPublisher) {
        this.eventPublisher = Objects.requireNonNull(eventPublisher);
    }

    @Override
    public NotificacaoPrazoDispatchResult publicarAlertaPrazo(NotificacaoPrazoCommand command) {
        Objects.requireNonNull(command);
        CalendarNotificationEnvelope envelope = new CalendarNotificationEnvelope(
                UUID.nameUUIDFromBytes(command.notificationKey().getBytes(StandardCharsets.UTF_8)),
                command.usuarioId(),
                command.processoId(),
                command.processoNumero(),
                "PROCESSUAL",
                "PRAZO",
                "PRAZO_PROCESSUAL",
                "ALERTA_PRAZO",
                command.prioridade(),
                cor(command.prioridade()),
                command.titulo(),
                command.corpo(),
                command.urlDetalhes(),
                "USUARIO",
                command.vencimentoForense().atStartOfDay(),
                command.notificarEm(),
                Instant.now(),
                command.notificationKey()
        );
        eventPublisher.publish(envelope);
        return new NotificacaoPrazoDispatchResult(true, "PUBLICADA", command.notificationKey(), command.prioridade());
    }

    private String cor(String prioridade) {
        if ("CRITICA".equalsIgnoreCase(prioridade)) {
            return "RED";
        }
        if ("ALTA".equalsIgnoreCase(prioridade)) {
            return "AMBER";
        }
        return "BLUE";
    }
}
