package com.tcc.pjb.backend.configs.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class PjbAdaptiveDataPlaneFilterTest {

    @Test
    void shouldEmitAdaptiveHeadersAndClearContext() throws Exception {
        PjbAdaptiveDataPlaneService service = mock(PjbAdaptiveDataPlaneService.class);
        PjbAdaptiveDataPlaneContext context = new PjbAdaptiveDataPlaneContext();
        PjbDataSourceRoutingProperties properties = new PjbDataSourceRoutingProperties();
        when(service.decide(org.mockito.ArgumentMatchers.any())).thenReturn(
                new PjbAdaptiveDataPlaneService.AdaptiveDecision(
                        PjbAdaptiveDataPlaneService.AdaptiveMode.CACHE_HOT,
                        "cache-hot-lane-preferred",
                        false,
                        true,
                        false,
                        false,
                        6.2d,
                        0.91d,
                        0.42d,
                        11,
                        2,
                        "READ_SUDESTE",
                        "NORDESTE",
                        true,
                        "SECRETARIA_TRIBUNAL",
                        "SEGUNDA_INSTANCIA",
                        "ESTADUAL"
                )
        );
        PjbAdaptiveDataPlaneFilter filter = new PjbAdaptiveDataPlaneFilter(service, context, properties);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/painel");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> assertThat(context.current()).isNotNull());
        assertThat(response.getHeader("X-PJB-Data-Plane-Mode")).isEqualTo("CACHE_HOT");
        assertThat(response.getHeader("X-PJB-Data-Plane-Cache-Recommended")).isEqualTo("true");
        assertThat(response.getHeader("X-PJB-Data-Plane-Preferred-Replica")).isEqualTo("READ_SUDESTE");
        assertThat(response.getHeader("X-PJB-Data-Plane-Sovereign-Scope")).isEqualTo("NORDESTE");
        assertThat(response.getHeader("X-PJB-Data-Plane-Scale-Profile")).isEqualTo("SECRETARIA_TRIBUNAL");
        assertThat(context.current()).isNull();
    }
}
