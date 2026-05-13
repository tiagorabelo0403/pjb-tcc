package com.tcc.pjb.backend.platform.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PjbRuntimeSizingPolicyTest {

    @Test
    void shouldBiasApiRoleTowardExternalIoAndLimitJob() {
        PjbRuntimeSizingPolicy.Footprint footprint = new PjbRuntimeSizingPolicy.Footprint(4, 2048);
        int apiExternalIo = PjbRuntimeSizingPolicy.clampLaneConcurrency("external-io", 256, footprint, "api");
        int apiJob = PjbRuntimeSizingPolicy.clampLaneConcurrency("job", 256, footprint, "api");
        int workerJob = PjbRuntimeSizingPolicy.clampLaneConcurrency("job", 256, footprint, "worker");
        assertThat(apiExternalIo).isGreaterThan(apiJob);
        assertThat(workerJob).isGreaterThan(apiJob);
    }
}
