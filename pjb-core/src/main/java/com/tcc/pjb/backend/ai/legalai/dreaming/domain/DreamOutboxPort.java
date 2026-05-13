package com.tcc.pjb.backend.ai.legalai.dreaming.domain;

import java.time.Instant;
import java.util.UUID;

public interface DreamOutboxPort {

    void registrar(UUID dreamId, String payloadJson, Instant criadoEm);
}
