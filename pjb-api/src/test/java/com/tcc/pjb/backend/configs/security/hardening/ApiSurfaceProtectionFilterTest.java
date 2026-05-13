package com.tcc.pjb.backend.configs.security.hardening;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ApiSurfaceProtectionFilterTest {

    @Test
    void shouldRejectTraversalToken() throws ServletException, IOException {
        ApiSurfaceProtectionProperties properties = new ApiSurfaceProtectionProperties();
        ApiSurfaceProtectionFilter filter = new ApiSurfaceProtectionFilter(properties);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/documentos/%2e%2e/segredo");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getContentAsString()).contains("path_traversal_token");
    }

    @Test
    void shouldRejectTooManyHeaders() throws ServletException, IOException {
        ApiSurfaceProtectionProperties properties = new ApiSurfaceProtectionProperties();
        properties.setMaxHeaderCount(2);
        ApiSurfaceProtectionFilter filter = new ApiSurfaceProtectionFilter(properties);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/processos");
        request.addHeader("X-A", "1");
        request.addHeader("X-B", "2");
        request.addHeader("X-C", "3");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getContentAsString()).contains("too_many_headers");
    }
}
