package com.tcc.pjb.backend.service.notification;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import com.tcc.pjb.backend.model.entity.notification.UserNotificationPreference;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class WebhookNotificationChannel implements NotificationChannel {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String channelId() {
        return "WEBHOOK";
    }

    @Override
    public boolean supports(UserNotificationPreference preference) {
        return preference != null && preference.isAllowWebhook() && preference.getWebhookUrl() != null && !preference.getWebhookUrl().isBlank();
    }

    @Override
    public void send(NotificationDispatchContext context) {
        try {
            String webhookUrl = context.preference() != null ? context.preference().getWebhookUrl() : null;
            ResponseEntity<String> response = restTemplate.postForEntity(
                    webhookUrl,
                    context.message(),
                    String.class
            );

            log.info("Webhook enviado. Status: {}", response.getStatusCode());
        } catch (Exception e) {
            log.error("Falha ao enviar webhook: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
