package com.tcc.pjb.backend.service.notification;

import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.model.entity.notification.UserNotificationPreference;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class WhatsAppNotificationChannel implements NotificationChannel {

    @Override
    public String channelId() {
        return "WHATSAPP";
    }

    @Override
    public boolean supports(UserNotificationPreference preference) {
        return preference == null || (preference.isAllowWhatsapp() && preference.getWhatsappNumber() != null && !preference.getWhatsappNumber().isBlank());
    }

    @Override
    public void send(NotificationDispatchContext context) {
        NotificationMessage message = context.message();
        UserNotificationPreference preference = context.preference();
        String destination = preference != null ? preference.getWhatsappNumber() : null;
        log.info("[NOTIFY][WHATSAPP] usuarioId={} processoId={} destino={} title={} tracking={}",
                message.destinatario() != null ? message.destinatario().getId() : null,
                message.processo() != null ? message.processo().getId() : null,
                destination,
                message.titulo(),
                context.trackingToken());
    }
}
