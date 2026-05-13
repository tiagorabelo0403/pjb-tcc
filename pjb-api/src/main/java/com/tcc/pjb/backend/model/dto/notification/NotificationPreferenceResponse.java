package com.tcc.pjb.backend.model.dto.notification;

public record NotificationPreferenceResponse(
        Long usuarioId,
        boolean allowEmail,
        boolean allowPush,
        boolean allowWhatsapp,
        boolean allowArDigital,
        boolean allowWebhook,
        boolean allowDigest,
        boolean onlyHighPriority,
        Integer antiSpamWindowMinutes,
        String pushEndpoint,
        String whatsappNumber,
        String arDigitalAddress,
        String webhookUrl
) {
}
