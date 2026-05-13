package com.tcc.pjb.backend.configs.live;

import java.time.Instant;

public record LiveClusterEvent(String topic,
                               long sequence,
                               String payload,
                               Instant createdAt) {
}
