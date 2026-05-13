package com.tcc.pjb.backend.core.observability;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestCorrelationFilterTest {

    @Test
    void shouldSanitizeUnsafeRequestId() throws ServletException, IOException {
        RequestCorrelationFilter filter = new RequestCorrelationFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/processos");
        request.addHeader(RequestCorrelationFilter.HEADER_REQUEST_ID, "  abcd<>/\\efgh1234567890  ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(RequestCorrelationFilter.HEADER_REQUEST_ID)).isEqualTo("abcdefgh1234567890");
    }
}
