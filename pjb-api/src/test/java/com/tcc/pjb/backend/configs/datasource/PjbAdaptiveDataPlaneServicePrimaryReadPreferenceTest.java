package com.tcc.pjb.backend.configs.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.configs.datasource.PjbAdaptiveDataPlaneService.AdaptiveMode;
import java.time.Duration;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;

class PjbAdaptiveDataPlaneServicePrimaryReadPreferenceTest {

    @AfterEach
    void clearContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void shouldForcePrimaryWhenPrimaryReadPreferenceWindowIsActive() {
        PjbDataSourceRoutingProperties properties = new PjbDataSourceRoutingProperties();
        properties.getAdaptivePlane().setEnabled(true);
        PjbPrimaryReadPreferenceContext primaryReadPreferenceContext = new PjbPrimaryReadPreferenceContext();
        primaryReadPreferenceContext.preferPrimaryFor(Duration.ofSeconds(5));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/processual/consulta");

        PjbAdaptiveDataPlaneService service = new PjbAdaptiveDataPlaneService(
                properties,
                primaryReadPreferenceContext,
                provider(PjbReplicaObservationService.class, null),
                provider(DataSource.class, null),
                provider(DataSource.class, null),
                provider(DataSource.class, null),
                provider(PjbSovereignFallbackResolver.class, null),
                provider(com.tcc.pjb.backend.service.infra.scaling.JudicialScaleProfileResolver.class, null),
                provider(ReadAfterWriteConsistencyPolicy.class, null)
        );

        var decision = service.decide(request);

        assertThat(decision.mode()).isEqualTo(AdaptiveMode.PRIMARY_STRICT);
        assertThat(decision.forcePrimary()).isTrue();
        assertThat(decision.reason()).isEqualTo("read-your-writes-window");
    }

    private static <T> ObjectProvider<T> provider(Class<T> type, T bean) {
        StaticListableBeanFactory factory = new StaticListableBeanFactory();
        if (bean != null) {
            factory.addBean("bean-" + type.getSimpleName(), bean);
        }
        return factory.getBeanProvider(type);
    }
}
