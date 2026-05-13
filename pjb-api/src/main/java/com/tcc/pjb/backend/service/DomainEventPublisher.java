package com.tcc.pjb.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class DomainEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(DomainEventPublisher.class);

    private final ApplicationEventPublisher publisher;

    public DomainEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void publish(Object event) {
        if (event == null) {
            logger.warn("Tentativa de publicar evento nulo ignorada.");
            return;
        }
        logger.info("Publicando evento de domínio: {}", event.getClass().getSimpleName());
        publisher.publishEvent(event);
    }
}
