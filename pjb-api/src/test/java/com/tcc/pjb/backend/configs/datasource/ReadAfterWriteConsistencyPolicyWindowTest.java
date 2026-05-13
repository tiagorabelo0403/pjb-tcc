package com.tcc.pjb.backend.configs.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class ReadAfterWriteConsistencyPolicyWindowTest {

    @Test
    void naoDeveForcarPrimarySemRequest() {
        ReadAfterWriteConsistencyPolicy policy = new ReadAfterWriteConsistencyPolicy();
        RequestContextHolder.resetRequestAttributes();
        assertThat(policy.shouldForcePrimary()).isFalse();
    }

    @Test
    void deveExpirarMarcacaoAntiga() {
        ReadAfterWriteConsistencyPolicy policy = new ReadAfterWriteConsistencyPolicy();
        MockHttpServletRequest request = new MockHttpServletRequest();
        ServletRequestAttributes attrs = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(attrs);
        try {
            attrs.setAttribute("pjb.raw.write.at", System.currentTimeMillis() - 5_000L, ServletRequestAttributes.SCOPE_REQUEST);
            assertThat(policy.shouldForcePrimary()).isFalse();
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }
}
