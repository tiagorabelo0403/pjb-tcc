package com.tcc.pjb.backend.configs.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class PjbCorrelationFilterTest {

    private final PjbCorrelationFilter filter = new PjbCorrelationFilter();

    @Test
    void geraCorrelationIdQuandoHeaderAusente() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        String header = response.getHeader("X-PJB-Correlation-Id");
        assertThat(header).isNotNull().isNotBlank();
    }

    @Test
    void reutilizaCorrelationIdForecidoPeloCliente() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-PJB-Correlation-Id", "meu-id-custom");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getHeader("X-PJB-Correlation-Id")).isEqualTo("meu-id-custom");
    }

    @Test
    void correlationIdGeradoEhUuidV7() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        String id = response.getHeader("X-PJB-Correlation-Id");
        java.util.UUID uuid = java.util.UUID.fromString(id);
        assertThat((uuid.getMostSignificantBits() >> 12) & 0xF).isEqualTo(7L);
    }

    @Test
    void mdcLimpoAposRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);
        doAnswer(inv -> {
            assertThat(org.slf4j.MDC.get("correlationId")).isNotNull();
            return null;
        }).when(chain).doFilter(request, response);

        filter.doFilterInternal(request, response, chain);

        assertThat(org.slf4j.MDC.get("correlationId")).isNull();
    }
}
