package com.tcc.pjb.backend.ai.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.guard.MockGuardEnvironmentQuery;
import com.tcc.pjb.backend.core.guard.MockGuardProfile;
import com.tcc.pjb.backend.core.guard.MockGuardViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class VectorSearchMockGuardTest {

    @Test
    void construtorComAmbienteReal_lancaViolation() {
        MockGuardEnvironmentQuery query = mock(MockGuardEnvironmentQuery.class);
        when(query.isRealEnvironment()).thenReturn(true);
        when(query.activeGuardProfile()).thenReturn(MockGuardProfile.PROD);
        doNothing().when(query).recordViolation(any());

        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        doNothing().when(publisher).publishEvent(any());

        assertThatThrownBy(() -> new VectorSearchServiceMock(query, publisher))
                .isInstanceOf(MockGuardViolationException.class)
                .hasMessageContaining("pjb.ai.vector.mode");
    }

    @Test
    void construtorComAmbienteNaoReal_instanciaERetornaDados() {
        MockGuardEnvironmentQuery query = mock(MockGuardEnvironmentQuery.class);
        when(query.isRealEnvironment()).thenReturn(false);

        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);

        VectorSearchServiceMock mock = new VectorSearchServiceMock(query, publisher);
        VectorSearchService.VectorSearchResult result = mock.searchSimilarResult("tutela urgência", null, 3);

        assertThat(result).isNotNull();
        assertThat(result.iaVersion()).isEqualTo("mock-v3");
    }
}
