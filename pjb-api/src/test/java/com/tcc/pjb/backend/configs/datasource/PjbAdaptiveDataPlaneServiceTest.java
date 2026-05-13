package com.tcc.pjb.backend.configs.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import com.tcc.pjb.backend.service.infra.scaling.JudicialScaleProfileResolver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.mock.web.MockHttpServletRequest;

class PjbAdaptiveDataPlaneServiceTest {

    @Test
    void shouldForcePrimaryAndRecommendQueueForAsyncMutatingRoute() {
        PjbAdaptiveDataPlaneService service = new PjbAdaptiveDataPlaneService(
                new PjbDataSourceRoutingProperties(),
                new PjbPrimaryReadPreferenceContext(),
                provider(PjbReplicaObservationService.class, null),
                provider(DataSource.class, null),
                provider(DataSource.class, null),
                provider(DataSource.class, null),
                provider(PjbSovereignFallbackResolver.class, null),
                provider(JudicialScaleProfileResolver.class, new JudicialScaleProfileResolver()),
                provider(ReadAfterWriteConsistencyPolicy.class, null)
        );
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/documentos/upload/lote");
        PjbAdaptiveDataPlaneService.AdaptiveDecision decision = service.decide(request);
        assertThat(decision.mode()).isEqualTo(PjbAdaptiveDataPlaneService.AdaptiveMode.QUEUE_DEFERRED);
        assertThat(decision.forcePrimary()).isTrue();
        assertThat(decision.asyncRecommended()).isTrue();
    }

    @Test
    void shouldPreferPrimaryOnCriticalReadWhenReplicaLagIsHigh() {
        PjbReplicaObservationService observationService = mock(PjbReplicaObservationService.class);
        when(observationService.currentSnapshot()).thenReturn(snapshot(4.5d, true, Map.of()));
        PjbAdaptiveDataPlaneService service = new PjbAdaptiveDataPlaneService(
                new PjbDataSourceRoutingProperties(),
                new PjbPrimaryReadPreferenceContext(),
                provider(PjbReplicaObservationService.class, observationService),
                provider(DataSource.class, null),
                provider(DataSource.class, null),
                provider(DataSource.class, null),
                provider(PjbSovereignFallbackResolver.class, null),
                provider(JudicialScaleProfileResolver.class, new JudicialScaleProfileResolver()),
                provider(ReadAfterWriteConsistencyPolicy.class, null)
        );
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/processos/123");
        PjbAdaptiveDataPlaneService.AdaptiveDecision decision = service.decide(request);
        assertThat(decision.mode()).isEqualTo(PjbAdaptiveDataPlaneService.AdaptiveMode.PRIMARY_STRICT);
        assertThat(decision.forcePrimary()).isTrue();
    }

    @Test
    void shouldPreferSearchIndexForSearchRouteWhenReplicaLagIsDegraded() {
        PjbReplicaObservationService observationService = mock(PjbReplicaObservationService.class);
        when(observationService.currentSnapshot()).thenReturn(snapshot(7.5d, true, Map.of()));
        PjbAdaptiveDataPlaneService service = new PjbAdaptiveDataPlaneService(
                new PjbDataSourceRoutingProperties(),
                new PjbPrimaryReadPreferenceContext(),
                provider(PjbReplicaObservationService.class, observationService),
                provider(DataSource.class, null),
                provider(DataSource.class, null),
                provider(DataSource.class, null),
                provider(PjbSovereignFallbackResolver.class, null),
                provider(JudicialScaleProfileResolver.class, new JudicialScaleProfileResolver()),
                provider(ReadAfterWriteConsistencyPolicy.class, null)
        );
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/busca/processos");
        PjbAdaptiveDataPlaneService.AdaptiveDecision decision = service.decide(request);
        assertThat(decision.mode()).isEqualTo(PjbAdaptiveDataPlaneService.AdaptiveMode.SEARCH_INDEX);
        assertThat(decision.searchRecommended()).isTrue();
    }

    @Test
    void shouldPreferRegionalReplicaWhenRegionalNodesAreAvailable() {
        PjbReplicaObservationService observationService = mock(PjbReplicaObservationService.class);
        when(observationService.currentSnapshot()).thenReturn(snapshot(0.5d, true, Map.of(
                "READ_NORDESTE", new PjbReplicaObservationService.ReplicaNodeSnapshot("READ_NORDESTE", "jdbc:postgresql://ne/pjb", true, true, true, 0.3d, null)
        )));
        PjbAdaptiveDataPlaneService service = new PjbAdaptiveDataPlaneService(
                new PjbDataSourceRoutingProperties(),
                new PjbPrimaryReadPreferenceContext(),
                provider(PjbReplicaObservationService.class, observationService),
                provider(DataSource.class, null),
                provider(DataSource.class, null),
                provider(DataSource.class, null),
                provider(PjbSovereignFallbackResolver.class, null),
                provider(JudicialScaleProfileResolver.class, new JudicialScaleProfileResolver()),
                provider(ReadAfterWriteConsistencyPolicy.class, null)
        );
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/painel/advocacia");
        PjbAdaptiveDataPlaneService.AdaptiveDecision decision = service.decide(request);
        assertThat(decision.mode()).isEqualTo(PjbAdaptiveDataPlaneService.AdaptiveMode.REPLICA_REGIONAL);
        assertThat(decision.forcePrimary()).isFalse();
    }


    @Test
    void shouldSelectSovereignFallbackReplicaForRegionalRead() {
        PjbReplicaObservationService observationService = mock(PjbReplicaObservationService.class);
        when(observationService.currentSnapshot()).thenReturn(snapshot(0.5d, true, Map.of(
                "READ_SUDESTE", new PjbReplicaObservationService.ReplicaNodeSnapshot("READ_SUDESTE", "jdbc:postgresql://se/pjb", true, true, true, 0.2d, null)
        )));
        PjbDataSourceRoutingProperties properties = new PjbDataSourceRoutingProperties();
        PjbAdaptiveDataPlaneService service = new PjbAdaptiveDataPlaneService(
                properties,
                new PjbPrimaryReadPreferenceContext(),
                provider(PjbReplicaObservationService.class, observationService),
                provider(DataSource.class, null),
                provider(DataSource.class, null),
                provider(DataSource.class, null),
                provider(PjbSovereignFallbackResolver.class, new PjbSovereignFallbackResolver(properties)),
                provider(JudicialScaleProfileResolver.class, new JudicialScaleProfileResolver()),
                provider(ReadAfterWriteConsistencyPolicy.class, null)
        );
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/painel/advocacia");
        request.addHeader(properties.getRegionalSelection().getRequestHeaderUf(), "CE");
        PjbAdaptiveDataPlaneService.AdaptiveDecision decision = service.decide(request);
        assertThat(decision.mode()).isEqualTo(PjbAdaptiveDataPlaneService.AdaptiveMode.REPLICA_REGIONAL);
        assertThat(decision.preferredReplicaKey()).isEqualTo("READ_SUDESTE");
        assertThat(decision.sovereignFallbackActivated()).isTrue();
    }


    @Test
    void shouldForcePrimaryDuringReadAfterWriteWindow() {
        ReadAfterWriteConsistencyPolicy raw = new ReadAfterWriteConsistencyPolicy();
        org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(new org.springframework.web.context.request.ServletRequestAttributes(new MockHttpServletRequest()));
        try {
            raw.markWrite();
            PjbAdaptiveDataPlaneService service = new PjbAdaptiveDataPlaneService(
                    new PjbDataSourceRoutingProperties(),
                    new PjbPrimaryReadPreferenceContext(),
                    provider(PjbReplicaObservationService.class, null),
                    provider(DataSource.class, null),
                    provider(DataSource.class, null),
                    provider(DataSource.class, null),
                    provider(PjbSovereignFallbackResolver.class, null),
                    provider(JudicialScaleProfileResolver.class, new JudicialScaleProfileResolver()),
                    provider(ReadAfterWriteConsistencyPolicy.class, raw)
            );
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/processos/123");
            PjbAdaptiveDataPlaneService.AdaptiveDecision decision = service.decide(request);
            assertThat(decision.mode()).isEqualTo(PjbAdaptiveDataPlaneService.AdaptiveMode.PRIMARY_STRICT);
            assertThat(decision.forcePrimary()).isTrue();
        } finally {
            org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes();
        }
    }

    private static PjbReplicaObservationService.ReplicaObservationSnapshot snapshot(double lagSeconds,
                                                                                    boolean replicaAvailable,
                                                                                    Map<String, PjbReplicaObservationService.ReplicaNodeSnapshot> regional) {
        return new PjbReplicaObservationService.ReplicaObservationSnapshot(
                Instant.now(),
                new PjbReplicaObservationService.ReplicaNodeSnapshot("WRITE", "jdbc:postgresql://primary/pjb", true, true, false, null, null),
                new PjbReplicaObservationService.ReplicaNodeSnapshot("READ", "jdbc:postgresql://replica/pjb", replicaAvailable, true, true, lagSeconds, null),
                regional,
                replicaAvailable,
                0L,
                10L,
                0L,
                regional.size(),
                regional.values().stream().filter(PjbReplicaObservationService.ReplicaNodeSnapshot::available).count()
        );
    }

    private static <T> ObjectProvider<T> provider(Class<T> type, T instance) {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        if (instance != null) {
            beanFactory.registerSingleton(type.getSimpleName(), instance);
        }
        return beanFactory.getBeanProvider(type);
    }
}
