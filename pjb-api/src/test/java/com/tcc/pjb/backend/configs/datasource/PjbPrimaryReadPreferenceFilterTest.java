package com.tcc.pjb.backend.configs.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class PjbPrimaryReadPreferenceFilterTest {

    @Test
    void shouldHydratePrimaryPreferenceFromCookie() throws ServletException, IOException {
        PjbPrimaryReadPreferenceContext context = new PjbPrimaryReadPreferenceContext();
        PjbDataSourceRoutingProperties properties = new PjbDataSourceRoutingProperties();
        properties.getPropagation().setSecureCookie(false);
        PjbPrimaryReadPreferenceFilter filter = new PjbPrimaryReadPreferenceFilter(context, properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        long deadline = System.currentTimeMillis() + 5_000L;
        request.setCookies(new jakarta.servlet.http.Cookie(properties.getPropagation().getCookieName(), Long.toString(deadline)));
        FilterChain chain = (req, res) -> assertThat(context.isPrimaryPreferred()).isTrue();

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(properties.getPropagation().getResponseHeaderName())).isEqualTo(Long.toString(deadline));
        assertThat(response.getHeaders("Set-Cookie")).anyMatch(value -> value.contains(properties.getPropagation().getCookieName()));
    }

    @Test
    void shouldPropagatePrimaryPreferenceToResponseWhenRaisedDuringRequest() throws ServletException, IOException {
        PjbPrimaryReadPreferenceContext context = new PjbPrimaryReadPreferenceContext();
        PjbDataSourceRoutingProperties properties = new PjbDataSourceRoutingProperties();
        properties.getPropagation().setSecureCookie(false);
        PjbPrimaryReadPreferenceFilter filter = new PjbPrimaryReadPreferenceFilter(context, properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> context.preferPrimaryFor(java.time.Duration.ofSeconds(3));

        filter.doFilter(request, response, chain);

        long deadline = Long.parseLong(response.getHeader(properties.getPropagation().getResponseHeaderName()));
        assertThat(deadline).isGreaterThan(System.currentTimeMillis());
        assertThat(response.getHeaders("Set-Cookie")).anyMatch(value -> value.contains(properties.getPropagation().getCookieName()));
    }
}
