package com.tcc.pjb.backend.configs.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletRequest;
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
        HttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        ReadAfterWriteConsistencyPolicy policy = new ReadAfterWriteConsistencyPolicy();

        policy.markWrite();

        assertThat(policy.shouldForcePrimary()).isTrue();
    }

    @Test
    void shouldReturnFalseWhenRequestAttributesCleared() {
        HttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        ReadAfterWriteConsistencyPolicy policy = new ReadAfterWriteConsistencyPolicy();
        policy.markWrite();
        RequestContextHolder.resetRequestAttributes();

        assertThat(policy.shouldForcePrimary()).isFalse();
    }
}
