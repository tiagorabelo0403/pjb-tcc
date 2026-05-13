package com.tcc.pjb.backend.core.dje.domain;

import java.time.Instant;

public record DjePublicationHealthSnapshot(Long djeId, String status, Instant capturedAt) {}
