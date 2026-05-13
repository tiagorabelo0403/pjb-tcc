package com.tcc.pjb.backend.service.notification;

import com.tcc.pjb.backend.model.entity.notification.UserNotificationPreference;

public record NotificationDispatchContext(
        NotificationMessage message,
        UserNotificationPreference preference,
        String trackingToken,
        String pixelUrl,
        String cienciaUrl
) {
}
