package com.tcc.pjb.backend.configs.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class PjbPrimaryReadPreferenceWebFlowTest {

    @Test
    void shouldKeepReadYourWritesAcrossRequestsUsingPropagationCookie() throws Exception {
        PjbPrimaryReadPreferenceContext context = new PjbPrimaryReadPreferenceContext();
        PjbDataSourceRoutingProperties properties = new PjbDataSourceRoutingProperties();
        properties.getPropagation().setSecureCookie(false);
        properties.setReadYourWritesWindow(Duration.ofSeconds(5));
        PjbPrimaryReadPreferenceFilter filter = new PjbPrimaryReadPreferenceFilter(context, properties);
        PjbReadReplicaRoutingDataSource routingDataSource = new PjbReadReplicaRoutingDataSource(context);

        MockHttpServletRequest firstRequest = new MockHttpServletRequest();
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        FilterChain firstChain = (req, res) -> context.preferPrimaryFor(Duration.ofSeconds(3));
        filter.doFilter(firstRequest, firstResponse, firstChain);
        String setCookie = firstResponse.getHeaders("Set-Cookie").stream()
                .filter(value -> value.contains(properties.getPropagation().getCookieName()))
                .findFirst()
                .orElseThrow();
        String cookieValue = extractCookieValue(setCookie);

        MockHttpServletRequest secondRequest = new MockHttpServletRequest();
        secondRequest.setCookies(new jakarta.servlet.http.Cookie(properties.getPropagation().getCookieName(), cookieValue));
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        FilterChain secondChain = routeAsReadOnly(routingDataSource);

        filter.doFilter(secondRequest, secondResponse, secondChain);

        assertThat(secondResponse.getHeader(properties.getPropagation().getResponseHeaderName())).isEqualTo(cookieValue);
    }

    private FilterChain routeAsReadOnly(PjbReadReplicaRoutingDataSource routingDataSource) {
        return (req, res) -> {
            TransactionSynchronizationManager.setCurrentTransactionReadOnly(true);
            try {
                assertThat(routingDataSource.determineCurrentLookupKey()).isEqualTo(PjbDataSourceRole.WRITE);
            } finally {
                TransactionSynchronizationManager.setCurrentTransactionReadOnly(false);
            }
        };
    }

    private String extractCookieValue(String setCookie) throws IOException {
        int start = setCookie.indexOf('=');
        int end = setCookie.indexOf(';');
        if (start < 0) {
            throw new IOException("cookie invalido");
        }
        return end > start ? setCookie.substring(start + 1, end) : setCookie.substring(start + 1);
    }
}
