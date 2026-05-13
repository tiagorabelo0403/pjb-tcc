package com.tcc.pjb.backend.configs.security.governance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.security.perimeter.ClientIpResolver;
import com.tcc.pjb.backend.service.infra.scaling.JudicialScaleProfileResolver;
import com.tcc.pjb.backend.service.security.ratelimit.RateLimiterStore;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ApiRouteGovernanceFilterTest {

    @Test
    void shouldScaleRateLimitByJudicialProfile() throws Exception {
        ApiRouteGovernanceProperties properties = new ApiRouteGovernanceProperties();
        ApiRouteGovernanceProperties.Rule rule = new ApiRouteGovernanceProperties.Rule();
        rule.setName("secretaria-operacional");
        rule.setPaths(java.util.List.of("/api/v1/secretaria/**"));
        rule.setMethods(java.util.List.of("GET"));
        rule.setMaxRequestsPerWindow(100);
        rule.setRateWindowSeconds(60);
        properties.getRules().add(rule);

        ClientIpResolver clientIpResolver = mock(ClientIpResolver.class);
        when(clientIpResolver.resolve(org.mockito.ArgumentMatchers.any())).thenReturn("127.0.0.1");
        RateLimiterStore rateLimiterStore = mock(RateLimiterStore.class);
        when(rateLimiterStore.incr(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(1L);

        ApiRouteGovernanceFilter filter = new ApiRouteGovernanceFilter(
                properties,
                clientIpResolver,
                rateLimiterStore,
                new JudicialScaleProfileResolver()
        );

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/secretaria/fila");
        request.addHeader(JudicialScaleProfileResolver.HEADER_INSTANCIA, "TRIBUNAL_SUPERIOR");
        request.addHeader(JudicialScaleProfileResolver.HEADER_RAMO, "FEDERAL");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
        });

        assertThat(response.getHeader("X-PJB-Route-Scale-Profile")).isEqualTo("SECRETARIA_TRIBUNAL_SUPERIOR");
        assertThat(Long.parseLong(response.getHeader("X-PJB-Route-Rate-Limit"))).isGreaterThan(100L);
    }

    @Test
    void shouldRejectWhenRateLimitWindowIsExceeded() throws Exception {
        ApiRouteGovernanceProperties properties = new ApiRouteGovernanceProperties();
        ApiRouteGovernanceProperties.Rule rule = new ApiRouteGovernanceProperties.Rule();
        rule.setName("secretaria-operacional");
        rule.setPaths(java.util.List.of("/api/v1/secretaria/**"));
        rule.setMethods(java.util.List.of("GET"));
        rule.setMaxRequestsPerWindow(1);
        rule.setRateWindowSeconds(60);
        properties.getRules().add(rule);

        ClientIpResolver clientIpResolver = mock(ClientIpResolver.class);
        when(clientIpResolver.resolve(org.mockito.ArgumentMatchers.any())).thenReturn("127.0.0.1");
        RateLimiterStore rateLimiterStore = mock(RateLimiterStore.class);
        when(rateLimiterStore.incr(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(2L);

        ApiRouteGovernanceFilter filter = new ApiRouteGovernanceFilter(
                properties,
                clientIpResolver,
                rateLimiterStore,
                new JudicialScaleProfileResolver()
        );

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/secretaria/fila");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            throw new AssertionError("chain should not be invoked when route is rate limited");
        });

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isNotBlank();
        assertThat(response.getHeader("RateLimit-Limit")).isEqualTo("1");
        assertThat(response.getContentAsString()).contains("route_rate_limited", "effectiveRateLimit");
    }

}
