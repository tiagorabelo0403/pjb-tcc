package com.tcc.pjb.backend.core.dje.domain;

import java.time.Instant;

public record DjeNotificationHealthSnapshot(Long djeId, boolean notificadas, Instant capturedAt) {}
