package com.tcc.pjb.backend.service.notification;

import com.tcc.pjb.backend.model.entity.notification.UserNotificationPreference;

public interface NotificationChannel {

    default String channelId() {
        return getClass().getSimpleName();
    }

    default boolean supports(UserNotificationPreference preference) {
        return true;
    }

    void send(NotificationDispatchContext context);
}
