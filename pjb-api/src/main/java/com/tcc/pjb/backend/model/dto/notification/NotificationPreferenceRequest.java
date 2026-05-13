package com.tcc.pjb.backend.model.dto.notification;

public record NotificationPreferenceRequest(
        Boolean allowEmail,
        Boolean allowPush,
        Boolean allowWhatsapp,
        Boolean allowArDigital,
        Boolean allowWebhook,
        Boolean allowDigest,
        Boolean onlyHighPriority,
        Integer antiSpamWindowMinutes,
        String pushEndpoint,
        String whatsappNumber,
        String arDigitalAddress,
        String webhookUrl
) {
}
