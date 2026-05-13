package com.tcc.pjb.backend.service.notification;

import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.model.entity.notification.UserNotificationPreference;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PushNotificationChannel implements NotificationChannel {

    @Override
    public String channelId() {
        return "PUSH";
    }

    @Override
    public boolean supports(UserNotificationPreference preference) {
        return preference == null || (preference.isAllowPush() && preference.getPushEndpoint() != null && !preference.getPushEndpoint().isBlank());
    }

    @Override
    public void send(NotificationDispatchContext context) {
        NotificationMessage message = context.message();
        UserNotificationPreference preference = context.preference();
        log.info("[NOTIFY][PUSH] usuarioId={} processoId={} endpointHash={} title={} tracking={}",
                message.destinatario() != null ? message.destinatario().getId() : null,
                message.processo() != null ? message.processo().getId() : null,
                preference != null && preference.getPushEndpoint() != null ? Integer.toHexString(preference.getPushEndpoint().hashCode()) : null,
                message.titulo(),
                context.trackingToken());
    }
}
