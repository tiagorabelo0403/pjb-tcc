package com.tcc.pjb.backend.ai.legalai.dreaming.infra;

import com.tcc.pjb.backend.ai.legalai.dreaming.domain.DreamOutboxPort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class DreamOutboxJpaAdapter implements DreamOutboxPort {

    private final DreamOutboxJpaRepository repository;

    public DreamOutboxJpaAdapter(DreamOutboxJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void registrar(UUID dreamId, String payloadJson, Instant criadoEm) {
        DreamOutboxJpaEntity entity = new DreamOutboxJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setDreamId(dreamId);
        entity.setPayload(payloadJson);
        entity.setProcessado(false);
        entity.setCriadoEm(criadoEm);
        repository.save(entity);
    }
}
