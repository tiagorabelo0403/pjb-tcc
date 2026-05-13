package com.tcc.pjb.backend.service.secretariat.live;

import java.time.Instant;
import java.util.Map;

public record SecretariatLiveMessage(
    String inboxKey,
    String type,
    Instant at,
    Map<String, Object> data
) {
}
