package com.tcc.pjb.backend.core.comunicacao.institucional.workflow.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.tcc.pjb.backend.core.comunicacao.institucional.audit.infrastructure.InstitutionalTimelineStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.delivery.infrastructure.InstitutionalDeliveryJobStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.infrastructure.InstitutionalInboxStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.infrastructure.InstitutionalDelegationAssignmentStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.infrastructure.InstitutionalDraftManifestationStateRepository;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class InstitutionalFlowAnalyticsApplicationServiceTest {

    @Test
    void shouldBoundScopedDashboardCache() throws Exception {
        InstitutionalInboxStateRepository inboxRepository = mock(InstitutionalInboxStateRepository.class);
        InstitutionalDeliveryJobStateRepository deliveryJobRepository = mock(InstitutionalDeliveryJobStateRepository.class);
        InstitutionalDelegationAssignmentStateRepository delegationRepository = mock(InstitutionalDelegationAssignmentStateRepository.class);
        InstitutionalDraftManifestationStateRepository draftRepository = mock(InstitutionalDraftManifestationStateRepository.class);
        InstitutionalTimelineStateRepository timelineRepository = mock(InstitutionalTimelineStateRepository.class);

        when(inboxRepository.findByProcessoId(org.mockito.ArgumentMatchers.anyLong())).thenReturn(List.of());
        when(deliveryJobRepository.findByProcessoId(org.mockito.ArgumentMatchers.anyLong())).thenReturn(List.of());
        when(delegationRepository.findByProcessoId(org.mockito.ArgumentMatchers.anyLong())).thenReturn(List.of());
        when(draftRepository.findByProcessoId(org.mockito.ArgumentMatchers.anyLong())).thenReturn(List.of());

        InstitutionalFlowAnalyticsApplicationService service = new InstitutionalFlowAnalyticsApplicationService(
                inboxRepository,
                deliveryJobRepository,
                delegationRepository,
                draftRepository,
                timelineRepository
        );

        for (long i = 1; i <= 320; i++) {
            service.dashboard(i, null);
        }

        Field cacheField = InstitutionalFlowAnalyticsApplicationService.class.getDeclaredField("scopedDashboardCache");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Object, ?> cache = (Map<Object, ?>) cacheField.get(service);

        assertThat(cache).hasSizeLessThanOrEqualTo(256);
    }
}
