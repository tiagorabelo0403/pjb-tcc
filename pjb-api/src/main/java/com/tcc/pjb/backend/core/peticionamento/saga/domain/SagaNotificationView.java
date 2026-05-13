package com.tcc.pjb.backend.core.peticionamento.saga.domain;

public record SagaNotificationView(
        Long rascunhoId,
        boolean notified,
        String channel
) {}
