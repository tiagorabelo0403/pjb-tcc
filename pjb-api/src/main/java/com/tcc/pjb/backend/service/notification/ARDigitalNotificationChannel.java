package com.tcc.pjb.backend.service.notification;

import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.model.entity.notification.UserNotificationPreference;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ARDigitalNotificationChannel implements NotificationChannel {

    @Override
    public String channelId() {
        return "AR_DIGITAL";
    }

    @Override
    public boolean supports(UserNotificationPreference preference) {
        return preference != null && preference.isAllowArDigital() && preference.getArDigitalAddress() != null && !preference.getArDigitalAddress().isBlank();
    }

    @Override
    public void send(NotificationDispatchContext context) {
        NotificationMessage message = context.message();
        UserNotificationPreference preference = context.preference();
        log.info("[NOTIFY][AR_DIGITAL] usuarioId={} processoId={} destino={} title={} tracking={}",
                message.destinatario() != null ? message.destinatario().getId() : null,
                message.processo() != null ? message.processo().getId() : null,
                preference != null ? preference.getArDigitalAddress() : null,
                message.titulo(),
                context.trackingToken());
    }
}
