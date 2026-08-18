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

class ReadAfterWriteConsistencyPolicyTest {

    @Test
    void shouldForcePrimaryAfterMarkWrite() {
        Clock fixedClock = Clock.fixed(Instant.now(), ZoneOffset.UTC);
        ReadAfterWriteConsistencyPolicy policy = new ReadAfterWriteConsistencyPolicy(fixedClock, Duration.ofSeconds(5));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        try {
            policy.markWrite();
            assertThat(policy.shouldForcePrimary()).isTrue();
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }
}
