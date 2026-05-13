package com.tcc.pjb.backend.configs.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class ReadAfterWriteConsistencyPolicyTest {

    @Test
    void shouldForcePrimaryAfterMarkWrite() {
        ReadAfterWriteConsistencyPolicy policy = new ReadAfterWriteConsistencyPolicy();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        try {
            policy.markWrite();
            assertThat(policy.shouldForcePrimary()).isTrue();
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }
}
