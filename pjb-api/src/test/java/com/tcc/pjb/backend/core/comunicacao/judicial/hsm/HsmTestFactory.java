package com.tcc.pjb.backend.core.comunicacao.judicial.hsm;

import com.tcc.pjb.backend.core.guard.MockGuardEnvironmentQuery;
import com.tcc.pjb.backend.core.guard.MockGuardProfile;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.mock.env.MockEnvironment;

public final class HsmTestFactory {

    private HsmTestFactory() {
    }

    public static PjbHardwareSecurityModule forTest(PjbHsmProperties props) {
        return new PjbHardwareSecurityModule(props, new NoOpMockGuardEnvironmentQuery(), event -> {});
    }

    public static final class NoOpMockGuardEnvironmentQuery extends MockGuardEnvironmentQuery {

        public NoOpMockGuardEnvironmentQuery() {
            super(new MockEnvironment(), new SimpleMeterRegistry());
        }

        @Override
        public boolean isRealEnvironment() {
            return false;
        }

        @Override
        public MockGuardProfile activeGuardProfile() {
            return MockGuardProfile.TEST;
        }

        @Override
        public void recordViolation(String service) {
            // no-op em testes
        }
    }
}
