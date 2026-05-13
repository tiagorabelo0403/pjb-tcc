package com.tcc.pjb.backend.service.notification;

import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.core.util.Hashes;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class LogNotificationChannel implements NotificationChannel {

    @Override
    public String channelId() {
        return "LOG";
    }

    @Override
    public void send(NotificationDispatchContext context) {
        NotificationMessage message = context.message();
        if (message == null) {
            log.warn("[NOTIFY][LOG] mensagem nula");
            return;
        }
        Long destId = message.destinatario() == null ? null : message.destinatario().getId();
        Long procId = message.processo() == null ? null : message.processo().getId();

        String title = message.titulo();
        String body = message.mensagem();
        String url = message.urlAcesso();

        log.info("[NOTIFY][LOG] destinatarioId={} processoId={} titleHash={} titleLen={} bodyHash={} bodyLen={} urlHash={} urlLen={} tracking={}",
                destId,
                procId,
                Hashes.sha256Hex(title),
                title == null ? 0 : title.length(),
                Hashes.sha256Hex(body),
                body == null ? 0 : body.length(),
                Hashes.sha256Hex(url),
                url == null ? 0 : url.length(),
                context.trackingToken()
        );
    }
}
