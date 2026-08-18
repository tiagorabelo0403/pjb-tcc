package com.tcc.pjb.backend.configs.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class ReadAfterWriteConsistencyPolicyWindowTest {

    @Test
    void naoDeveForcarPrimarySemRequest() {
        Clock fixedClock = Clock.fixed(Instant.now(), ZoneOffset.UTC);
        ReadAfterWriteConsistencyPolicy policy = new ReadAfterWriteConsistencyPolicy(fixedClock, Duration.ofSeconds(5));
        RequestContextHolder.resetRequestAttributes();
        assertThat(policy.shouldForcePrimary()).isFalse();
    }

    @Test
    void deveExpirarMarcacaoAntiga() {
        Clock fixedClock = Clock.fixed(Instant.now(), ZoneOffset.UTC);
        ReadAfterWriteConsistencyPolicy policy = new ReadAfterWriteConsistencyPolicy(fixedClock, Duration.ofSeconds(5));
        MockHttpServletRequest request = new MockHttpServletRequest();
        ServletRequestAttributes attrs = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(attrs);
        try {
            attrs.setAttribute("pjb.raw.write.at", fixedClock.millis() - 10_000L, ServletRequestAttributes.SCOPE_REQUEST);
            assertThat(policy.shouldForcePrimary()).isFalse();
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void windowMillisRetornaValorConfigurado() {
        Clock fixedClock = Clock.fixed(Instant.now(), ZoneOffset.UTC);
        ReadAfterWriteConsistencyPolicy policy = new ReadAfterWriteConsistencyPolicy(fixedClock, Duration.ofSeconds(7));
        assertThat(policy.windowMillis()).isEqualTo(7_000L);
    }

    @Test
    void deveForcarPrimaryDentroJanelaConfigurada() {
        Clock fixedClock = Clock.fixed(Instant.now(), ZoneOffset.UTC);
        ReadAfterWriteConsistencyPolicy policy = new ReadAfterWriteConsistencyPolicy(fixedClock, Duration.ofSeconds(5));
        MockHttpServletRequest request = new MockHttpServletRequest();
        ServletRequestAttributes attrs = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(attrs);
        try {
            attrs.setAttribute("pjb.raw.write.at", fixedClock.millis() - 3_000L, ServletRequestAttributes.SCOPE_REQUEST);
            assertThat(policy.shouldForcePrimary()).isTrue();
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }
}
