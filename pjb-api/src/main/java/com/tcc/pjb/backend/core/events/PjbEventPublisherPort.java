package com.tcc.pjb.backend.core.events;

public interface PjbEventPublisherPort {

    void publicar(PjbIntegrationEventEnvelope envelope);

    void publicarAsync(PjbIntegrationEventEnvelope envelope);
}
