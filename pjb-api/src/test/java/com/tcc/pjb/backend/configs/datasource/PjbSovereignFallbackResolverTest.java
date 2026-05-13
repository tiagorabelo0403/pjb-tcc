
package com.tcc.pjb.backend.configs.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class PjbSovereignFallbackResolverTest {

    @Test
    void shouldResolveFallbackReplicaByUfWhenRequestedRegionalReplicaIsUnavailable() {
        PjbDataSourceRoutingProperties properties = new PjbDataSourceRoutingProperties();
        PjbSovereignFallbackResolver resolver = new PjbSovereignFallbackResolver(properties);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/painel/advocacia");
        request.addHeader(properties.getRegionalSelection().getRequestHeaderUf(), "CE");
        PjbReplicaObservationService.ReplicaObservationSnapshot snapshot = new PjbReplicaObservationService.ReplicaObservationSnapshot(
                Instant.now(),
                new PjbReplicaObservationService.ReplicaNodeSnapshot("WRITE", "jdbc:postgresql://primary/pjb", true, true, false, null, null),
                new PjbReplicaObservationService.ReplicaNodeSnapshot("READ", "jdbc:postgresql://replica/pjb", true, true, true, 0.2d, null),
                Map.of("READ_SUDESTE", new PjbReplicaObservationService.ReplicaNodeSnapshot("READ_SUDESTE", "jdbc:postgresql://se/pjb", true, true, true, 0.1d, null)),
                true,
                0L,
                0L,
                0L,
                1L,
                1L
        );

        PjbSovereignFallbackResolver.SovereignResolution resolution = resolver.resolve(request, snapshot, false);

        assertThat(resolution.sovereignScope()).isEqualTo("NORDESTE");
        assertThat(resolution.preferredReplicaKey()).isEqualTo("READ_SUDESTE");
        assertThat(resolution.fallbackActivated()).isTrue();
        assertThat(resolution.forcePrimary()).isFalse();
    }

    @Test
    void shouldForcePrimaryOnCriticalExhaustion() {
        PjbDataSourceRoutingProperties properties = new PjbDataSourceRoutingProperties();
        PjbSovereignFallbackResolver resolver = new PjbSovereignFallbackResolver(properties);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/processos/123");
        request.addHeader(properties.getRegionalSelection().getRequestHeaderUf(), "CE");
        PjbReplicaObservationService.ReplicaObservationSnapshot snapshot = new PjbReplicaObservationService.ReplicaObservationSnapshot(
                Instant.now(),
                new PjbReplicaObservationService.ReplicaNodeSnapshot("WRITE", "jdbc:postgresql://primary/pjb", true, true, false, null, null),
                new PjbReplicaObservationService.ReplicaNodeSnapshot("READ", "jdbc:postgresql://replica/pjb", false, true, true, 8.5d, null),
                Map.of(),
                false,
                0L,
                0L,
                0L,
                0L,
                0L
        );

        PjbSovereignFallbackResolver.SovereignResolution resolution = resolver.resolve(request, snapshot, true);

        assertThat(resolution.forcePrimary()).isTrue();
        assertThat(resolution.fallbackActivated()).isTrue();
    }
}
