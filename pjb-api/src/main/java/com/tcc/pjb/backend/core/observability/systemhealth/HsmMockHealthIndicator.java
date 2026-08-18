package com.tcc.pjb.backend.core.observability.systemhealth;

import com.tcc.pjb.backend.core.comunicacao.judicial.hsm.PjbHsmProperties;
import com.tcc.pjb.backend.core.guard.MockGuardEnvironmentQuery;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("hsm-mock")
public class HsmMockHealthIndicator implements HealthIndicator {

    private final PjbHsmProperties hsmProps;
    private final MockGuardEnvironmentQuery mockGuardQuery;

    public HsmMockHealthIndicator(PjbHsmProperties hsmProps, MockGuardEnvironmentQuery mockGuardQuery) {
        this.hsmProps = Objects.requireNonNull(hsmProps, "hsmProps");
        this.mockGuardQuery = Objects.requireNonNull(mockGuardQuery, "mockGuardQuery");
    }

    @Override
    public Health health() {
        boolean mockEnabled = hsmProps.mockEnabled();
        boolean realEnv = mockGuardQuery.isRealEnvironment();
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("mockEnabled", mockEnabled);
        details.put("realEnvironment", realEnv);
        details.put("profile", mockGuardQuery.activeGuardProfile().name());

        if (mockEnabled && realEnv) {
            details.put("status", "HSM operando em modo MOCK em ambiente real — assinaturas sem validade ICP-Brasil");
            Health.Builder builder = Health.status("OUT_OF_SERVICE");
            details.forEach(builder::withDetail);
            return builder.build();
        }
        Health.Builder builder = Health.up();
        details.forEach(builder::withDetail);
        return builder.build();
    }
}
