package com.tcc.pjb.backend.configs.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class ReadAfterWriteConsistencyPolicyLifecycleTest {

    @AfterEach
    void clear() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void shouldForcePrimaryWithinRequestAfterMarkWrite() {
        Clock fixedClock = Clock.fixed(Instant.now(), ZoneOffset.UTC);
        HttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        ReadAfterWriteConsistencyPolicy policy = new ReadAfterWriteConsistencyPolicy(fixedClock, Duration.ofSeconds(5));

        policy.markWrite();

        assertThat(policy.shouldForcePrimary()).isTrue();
    }

    @Test
    void shouldReturnFalseWhenRequestAttributesCleared() {
        Clock fixedClock = Clock.fixed(Instant.now(), ZoneOffset.UTC);
        HttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        ReadAfterWriteConsistencyPolicy policy = new ReadAfterWriteConsistencyPolicy(fixedClock, Duration.ofSeconds(5));
        policy.markWrite();
        RequestContextHolder.resetRequestAttributes();

        assertThat(policy.shouldForcePrimary()).isFalse();
    }
}
