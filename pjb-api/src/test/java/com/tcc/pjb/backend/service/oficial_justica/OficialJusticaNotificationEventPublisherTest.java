package com.tcc.pjb.backend.service.oficial_justica;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaNotificationEnvelope;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;

class OficialJusticaNotificationEventPublisherTest {

    @Test
    void ignoresNullEnvelope() {
        @SuppressWarnings("unchecked")
        ObjectProvider<KafkaTemplate<String, Object>> provider = mock(ObjectProvider.class);
        OficialJusticaNotificationDispatchService dispatchService = mock(OficialJusticaNotificationDispatchService.class);
        OficialJusticaNotificationEventPublisher publisher = new OficialJusticaNotificationEventPublisher(provider, dispatchService, true);

        publisher.publish(null);

        verifyNoInteractions(dispatchService);
        verify(provider, never()).getIfAvailable();
    }

    @Test
    void ignoresEnvelopeWithoutUsuarioId() {
        @SuppressWarnings("unchecked")
        ObjectProvider<KafkaTemplate<String, Object>> provider = mock(ObjectProvider.class);
        OficialJusticaNotificationDispatchService dispatchService = mock(OficialJusticaNotificationDispatchService.class);
        OficialJusticaNotificationEventPublisher publisher = new OficialJusticaNotificationEventPublisher(provider, dispatchService, true);
        OficialJusticaNotificationEnvelope envelope = new OficialJusticaNotificationEnvelope(
                null,
                20L,
                30L,
                "ORDEM:30",
                "ORDEM_JUDICIAL_CUMPRIMENTO_OFICIAL",
                "ORDEM_JUDICIAL_OFICIAL:30",
                "Ordem judicial",
                "/api/v1/oficial-justica/processos-nomeados/20/workbench",
                true,
                "0001",
                "Fortaleza/CE",
                "MANDADO",
                "GABINETE",
                Instant.now()
        );

        publisher.publish(envelope);

        verifyNoInteractions(dispatchService);
        verify(provider, never()).getIfAvailable();
    }

    @Test
    void fallsBackToLocalDispatchWhenKafkaIsDisabled() {
        @SuppressWarnings("unchecked")
        ObjectProvider<KafkaTemplate<String, Object>> provider = mock(ObjectProvider.class);
        OficialJusticaNotificationDispatchService dispatchService = mock(OficialJusticaNotificationDispatchService.class);
        OficialJusticaNotificationEventPublisher publisher = new OficialJusticaNotificationEventPublisher(provider, dispatchService, false);

        OficialJusticaNotificationEnvelope envelope = new OficialJusticaNotificationEnvelope(
                10L,
                20L,
                30L,
                "NOMEACAO:30",
                "NOMEACAO_PROCESSUAL_OFICIAL",
                "NOMEACAO_OFICIAL:30",
                "Nova nomeação",
                "/api/v1/oficial-justica/processos-nomeados/20/workbench",
                true,
                "0001",
                null,
                "NOMEACAO",
                "SECRETARIA",
                Instant.now()
        );

        publisher.publish(envelope);

        verify(dispatchService).dispatch(envelope);
        verify(provider, never()).getIfAvailable();
    }

    @Test
    void fallsBackToLocalDispatchWhenKafkaIsEnabledButTemplateIsUnavailable() {
        @SuppressWarnings("unchecked")
        ObjectProvider<KafkaTemplate<String, Object>> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        OficialJusticaNotificationDispatchService dispatchService = mock(OficialJusticaNotificationDispatchService.class);
        OficialJusticaNotificationEventPublisher publisher = new OficialJusticaNotificationEventPublisher(provider, dispatchService, true);
        OficialJusticaNotificationEnvelope envelope = new OficialJusticaNotificationEnvelope(
                10L,
                20L,
                30L,
                "MANDADO:30",
                "MANDADO_JUDICIAL_OFICIAL",
                "MANDADO_OFICIAL:30",
                "Mandado judicial",
                "/api/v1/oficial-justica/processos-nomeados/20/workbench",
                true,
                "0001",
                "Fortaleza/CE",
                "MANDADO",
                "GABINETE",
                Instant.now()
        );

        publisher.publish(envelope);

        verify(provider).getIfAvailable();
        verify(dispatchService).dispatch(envelope);
    }

    @Test
    void publishesToKafkaWhenTemplateIsAvailable() {
        @SuppressWarnings("unchecked")
        ObjectProvider<KafkaTemplate<String, Object>> provider = mock(ObjectProvider.class);
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        when(provider.getIfAvailable()).thenReturn(kafkaTemplate);
        OficialJusticaNotificationDispatchService dispatchService = mock(OficialJusticaNotificationDispatchService.class);
        OficialJusticaNotificationEventPublisher publisher = new OficialJusticaNotificationEventPublisher(provider, dispatchService, true);

        OficialJusticaNotificationEnvelope envelope = new OficialJusticaNotificationEnvelope(
                10L,
                20L,
                30L,
                "ORDEM:30",
                "ORDEM_JUDICIAL_CUMPRIMENTO_OFICIAL",
                "ORDEM_JUDICIAL_OFICIAL:30",
                "Ordem judicial",
                "/api/v1/oficial-justica/processos-nomeados/20/workbench",
                true,
                "0001",
                "Fortaleza/CE",
                "MANDADO",
                "GABINETE",
                Instant.now()
        );

        publisher.publish(envelope);

        verify(provider).getIfAvailable();
        verify(kafkaTemplate).send(eq(OficialJusticaNotificationEventPublisher.TOPIC), eq("10"), eq(envelope));
        verify(dispatchService, never()).dispatch(any());
    }
}
