package com.tcc.pjb.backend.core.observability.systemhealth;

import com.tcc.pjb.backend.core.comunicacao.judicial.PjbBnmpProperties;
import com.tcc.pjb.backend.core.guard.MockGuardEnvironmentQuery;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("bnmp-mock")
public class BnmpMockHealthIndicator implements HealthIndicator {

    private final PjbBnmpProperties bnmpProps;
    private final MockGuardEnvironmentQuery mockGuardQuery;

    public BnmpMockHealthIndicator(PjbBnmpProperties bnmpProps, MockGuardEnvironmentQuery mockGuardQuery) {
        this.bnmpProps = Objects.requireNonNull(bnmpProps, "bnmpProps");
        this.mockGuardQuery = Objects.requireNonNull(mockGuardQuery, "mockGuardQuery");
    }

    @Override
    public Health health() {
        boolean mockEnabled = bnmpProps.mockEnabled();
        boolean realEnv = mockGuardQuery.isRealEnvironment();
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("mockEnabled", mockEnabled);
        details.put("realEnvironment", realEnv);
        details.put("profile", mockGuardQuery.activeGuardProfile().name());

        if (mockEnabled && realEnv) {
            details.put("status", "BNMP operando em modo MOCK em ambiente real — mandados com número fictício");
            Health.Builder builder = Health.status("OUT_OF_SERVICE");
            details.forEach(builder::withDetail);
            return builder.build();
        }
        Health.Builder builder = Health.up();
        details.forEach(builder::withDetail);
        return builder.build();
    }
}
