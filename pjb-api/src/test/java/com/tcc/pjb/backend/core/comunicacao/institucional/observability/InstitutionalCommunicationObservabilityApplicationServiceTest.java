package com.tcc.pjb.backend.core.comunicacao.institucional.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.comunicacao.institucional.delivery.infrastructure.InstitutionalDeliveryDeadLetterStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.delivery.infrastructure.InstitutionalDeliveryJobStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.gate.infrastructure.InstitutionalGateStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.infrastructure.InstitutionalInboxStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.integration.infrastructure.InstitutionalExternalDispatchStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.observability.application.InstitutionalCommunicationObservabilityApplicationService;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class InstitutionalCommunicationObservabilityApplicationServiceTest {

    @Test
    void shouldReuseScopedDashboardCacheForSameProcessScope() {
        InstitutionalDeliveryJobStateRepository jobRepository = mock(InstitutionalDeliveryJobStateRepository.class);
        InstitutionalDeliveryDeadLetterStateRepository deadLetterRepository = mock(InstitutionalDeliveryDeadLetterStateRepository.class);
        InstitutionalExternalDispatchStateRepository externalDispatchRepository = mock(InstitutionalExternalDispatchStateRepository.class);
        InstitutionalGateStateRepository gateRepository = mock(InstitutionalGateStateRepository.class);
        InstitutionalInboxStateRepository inboxRepository = mock(InstitutionalInboxStateRepository.class);
        when(jobRepository.findByProcessoId(42L)).thenReturn(List.of());
        when(deadLetterRepository.findByProcessoId(42L)).thenReturn(List.of());
        when(externalDispatchRepository.findByProcessoId(42L)).thenReturn(List.of());
        when(gateRepository.findByProcessoId(42L)).thenReturn(List.of());
        when(inboxRepository.findByProcessoId(42L)).thenReturn(List.of());

        InstitutionalCommunicationObservabilityApplicationService service = new InstitutionalCommunicationObservabilityApplicationService(
                jobRepository,
                deadLetterRepository,
                externalDispatchRepository,
                gateRepository,
                inboxRepository,
                new SimpleMeterRegistry()
        );

        service.dashboard(42L, null, null);
        service.dashboard(42L, null, null);

        verify(jobRepository, times(1)).findByProcessoId(42L);
        verify(deadLetterRepository, times(1)).findByProcessoId(42L);
        verify(externalDispatchRepository, times(1)).findByProcessoId(42L);
        verify(gateRepository, times(1)).findByProcessoId(42L);
        verify(inboxRepository, times(1)).findByProcessoId(42L);
    }

    @Test
    void shouldUseTargetedRepositoriesForKindOnlyScope() {
        InstitutionalDeliveryJobStateRepository jobRepository = mock(InstitutionalDeliveryJobStateRepository.class);
        InstitutionalDeliveryDeadLetterStateRepository deadLetterRepository = mock(InstitutionalDeliveryDeadLetterStateRepository.class);
        InstitutionalExternalDispatchStateRepository externalDispatchRepository = mock(InstitutionalExternalDispatchStateRepository.class);
        InstitutionalGateStateRepository gateRepository = mock(InstitutionalGateStateRepository.class);
        InstitutionalInboxStateRepository inboxRepository = mock(InstitutionalInboxStateRepository.class);
        when(jobRepository.findByDestinatarioKind(DestinatarioInstitucionalKind.MINISTERIO_PUBLICO)).thenReturn(List.of());
        when(externalDispatchRepository.findByDestinatarioKind(DestinatarioInstitucionalKind.MINISTERIO_PUBLICO)).thenReturn(List.of());
        when(gateRepository.findAll()).thenReturn(List.of());
        when(inboxRepository.findAll()).thenReturn(List.of());
        when(deadLetterRepository.countAll()).thenReturn(0L);

        InstitutionalCommunicationObservabilityApplicationService service = new InstitutionalCommunicationObservabilityApplicationService(
                jobRepository,
                deadLetterRepository,
                externalDispatchRepository,
                gateRepository,
                inboxRepository,
                new SimpleMeterRegistry()
        );

        service.dashboard(null, null, DestinatarioInstitucionalKind.MINISTERIO_PUBLICO);

        verify(jobRepository).findByDestinatarioKind(DestinatarioInstitucionalKind.MINISTERIO_PUBLICO);
        verify(externalDispatchRepository).findByDestinatarioKind(DestinatarioInstitucionalKind.MINISTERIO_PUBLICO);
        verify(jobRepository, never()).findAll();
        verify(externalDispatchRepository, never()).findAll();
    }

    @Test
    void shouldUseTargetedRepositoriesForUfScope() {
        InstitutionalDeliveryJobStateRepository jobRepository = mock(InstitutionalDeliveryJobStateRepository.class);
        InstitutionalDeliveryDeadLetterStateRepository deadLetterRepository = mock(InstitutionalDeliveryDeadLetterStateRepository.class);
        InstitutionalExternalDispatchStateRepository externalDispatchRepository = mock(InstitutionalExternalDispatchStateRepository.class);
        InstitutionalGateStateRepository gateRepository = mock(InstitutionalGateStateRepository.class);
        InstitutionalInboxStateRepository inboxRepository = mock(InstitutionalInboxStateRepository.class);
        when(jobRepository.findByUnidadeCodigoContainingIgnoreCase("CE")).thenReturn(List.of());
        when(externalDispatchRepository.findByUnidadeCodigoContainingIgnoreCase("CE")).thenReturn(List.of());
        when(gateRepository.findByGateCodeContainingIgnoreCase("CE")).thenReturn(List.of());
        when(inboxRepository.findByUnidadeCodigoContainingIgnoreCase("CE")).thenReturn(List.of());
        when(deadLetterRepository.countAll()).thenReturn(0L);

        InstitutionalCommunicationObservabilityApplicationService service = new InstitutionalCommunicationObservabilityApplicationService(
                jobRepository,
                deadLetterRepository,
                externalDispatchRepository,
                gateRepository,
                inboxRepository,
                new SimpleMeterRegistry()
        );

        service.dashboard(null, "ce", null);

        verify(jobRepository).findByUnidadeCodigoContainingIgnoreCase("CE");
        verify(externalDispatchRepository).findByUnidadeCodigoContainingIgnoreCase("CE");
        verify(gateRepository).findByGateCodeContainingIgnoreCase("CE");
        verify(inboxRepository).findByUnidadeCodigoContainingIgnoreCase("CE");
        verify(jobRepository, never()).findAll();
        verify(externalDispatchRepository, never()).findAll();
        verify(inboxRepository, never()).findAll();
    }

    @Test
    void shouldBoundScopedDashboardCacheCardinality() throws Exception {
        InstitutionalDeliveryJobStateRepository jobRepository = mock(InstitutionalDeliveryJobStateRepository.class);
        InstitutionalDeliveryDeadLetterStateRepository deadLetterRepository = mock(InstitutionalDeliveryDeadLetterStateRepository.class);
        InstitutionalExternalDispatchStateRepository externalDispatchRepository = mock(InstitutionalExternalDispatchStateRepository.class);
        InstitutionalGateStateRepository gateRepository = mock(InstitutionalGateStateRepository.class);
        InstitutionalInboxStateRepository inboxRepository = mock(InstitutionalInboxStateRepository.class);
        when(jobRepository.findByProcessoId(anyLong())).thenReturn(List.of());
        when(deadLetterRepository.findByProcessoId(anyLong())).thenReturn(List.of());
        when(externalDispatchRepository.findByProcessoId(anyLong())).thenReturn(List.of());
        when(gateRepository.findByProcessoId(anyLong())).thenReturn(List.of());
        when(inboxRepository.findByProcessoId(anyLong())).thenReturn(List.of());

        InstitutionalCommunicationObservabilityApplicationService service = new InstitutionalCommunicationObservabilityApplicationService(
                jobRepository,
                deadLetterRepository,
                externalDispatchRepository,
                gateRepository,
                inboxRepository,
                new SimpleMeterRegistry()
        );

        for (long i = 1; i <= 320; i++) {
            service.dashboard(i, null, null);
        }

        Field cacheField = InstitutionalCommunicationObservabilityApplicationService.class.getDeclaredField("scopedDashboardCache");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Object, ?> cache = (Map<Object, ?>) cacheField.get(service);

        assertThat(cache).hasSizeLessThanOrEqualTo(192);
    }
}
