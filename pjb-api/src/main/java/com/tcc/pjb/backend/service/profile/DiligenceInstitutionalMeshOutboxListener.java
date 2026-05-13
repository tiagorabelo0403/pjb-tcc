package com.tcc.pjb.backend.service.profile;

import java.time.Instant;
import java.util.Objects;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorMalhaInstitucionalDispatch;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorMalhaInstitucionalDispatchRepository;
import com.tcc.pjb.backend.service.outbox.OutboxGenericDispatchedEvent;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;

@Component
public class DiligenceInstitutionalMeshOutboxListener {

    private final DiligenciaOperadorMalhaInstitucionalDispatchRepository dispatchRepository;
    private final ObjectMapper objectMapper;

    public DiligenceInstitutionalMeshOutboxListener(DiligenciaOperadorMalhaInstitucionalDispatchRepository dispatchRepository,
                                                    ObjectMapper objectMapper) {
        this.dispatchRepository = Objects.requireNonNull(dispatchRepository);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @EventListener
    @Transactional
    public void onGenericDispatched(OutboxGenericDispatchedEvent event) {
        if (event == null || !OutboxPublisher.EVT_PROFILE_INSTITUTIONAL_MESH_DISPATCH.equals(event.eventType())) {
            return;
        }
        String replayToken = event.aggregateId();
        if (replayToken == null || replayToken.isBlank()) {
            replayToken = extractReplayToken(event.headersJson());
        }
        if (replayToken == null || replayToken.isBlank()) {
            return;
        }
        dispatchRepository.findByReplayToken(replayToken).ifPresent(this::markDispatched);
    }

    private void markDispatched(DiligenciaOperadorMalhaInstitucionalDispatch entity) {
        if (entity.getDeliveredAt() == null) {
            entity.setDeliveredAt(Instant.now());
        }
        if (!"ACKNOWLEDGED".equalsIgnoreCase(entity.getDispatchStatus())) {
            entity.setDispatchStatus("DISPATCHED");
        }
        dispatchRepository.save(entity);
    }

    private String extractReplayToken(String headersJson) {
        try {
            if (headersJson == null || headersJson.isBlank()) {
                return null;
            }
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> values = objectMapper.readValue(headersJson, java.util.Map.class);
            Object replayToken = values.get("replayToken");
            return replayToken != null ? String.valueOf(replayToken) : null;
        } catch (Exception ex) {
            return null;
        }
    }
}
