package com.tcc.pjb.backend.configs.security.hardening;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ApiSecurityHardeningFilterTest {

    @Test
    void shouldRejectTraceMethod() throws ServletException, IOException {
        SecurityHardeningProperties properties = new SecurityHardeningProperties();
        ApiSecurityHardeningFilter filter = new ApiSecurityHardeningFilter(properties);
        MockHttpServletRequest request = new MockHttpServletRequest("TRACE", "/api/v1/admin/metrics/replica-routing");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(405);
        assertThat(response.getContentType()).startsWith("application/problem+json");
        assertThat(response.getContentAsString()).contains("method_not_allowed");
    }

    @Test
    void shouldRejectMethodOverrideHeaders() throws ServletException, IOException {
        SecurityHardeningProperties properties = new SecurityHardeningProperties();
        ApiSecurityHardeningFilter filter = new ApiSecurityHardeningFilter(properties);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/processos");
        request.addHeader("X-HTTP-Method-Override", "DELETE");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getContentAsString()).contains("method_override_not_allowed");
    }

    @Test
    void shouldAddNoStoreAndBrowserHardeningHeadersOnSensitivePath() throws ServletException, IOException {
        SecurityHardeningProperties properties = new SecurityHardeningProperties();
        ApiSecurityHardeningFilter filter = new ApiSecurityHardeningFilter(properties);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin/health/internal");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader("Cache-Control")).contains("no-store");
        assertThat(response.getHeader("Pragma")).isEqualTo("no-cache");
        assertThat(response.getHeader("X-Permitted-Cross-Domain-Policies")).isEqualTo("none");
        assertThat(response.getHeader("X-DNS-Prefetch-Control")).isEqualTo("off");
        assertThat(response.getHeader("Origin-Agent-Cluster")).isEqualTo("?1");
        assertThat(response.getHeader("Vary")).contains("Origin").contains("Authorization").contains("X-Request-Id");
    }
}
