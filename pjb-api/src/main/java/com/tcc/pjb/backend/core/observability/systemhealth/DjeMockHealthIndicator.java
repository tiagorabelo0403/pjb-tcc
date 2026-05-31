package com.tcc.pjb.backend.core.observability.systemhealth;

import com.tcc.pjb.backend.core.dje.DjeProperties;
import com.tcc.pjb.backend.core.guard.MockGuardEnvironmentQuery;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("dje-mock")
public class DjeMockHealthIndicator implements HealthIndicator {

    private final DjeProperties djeProps;
    private final MockGuardEnvironmentQuery mockGuardQuery;

    public DjeMockHealthIndicator(DjeProperties djeProps, MockGuardEnvironmentQuery mockGuardQuery) {
        this.djeProps = Objects.requireNonNull(djeProps, "djeProps");
        this.mockGuardQuery = Objects.requireNonNull(mockGuardQuery, "mockGuardQuery");
    }

    @Override
    public Health health() {
        boolean mockEnabled = djeProps.mockEnabled();
        boolean realEnv = mockGuardQuery.isRealEnvironment();
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("mockEnabled", mockEnabled);
        details.put("realEnvironment", realEnv);
        details.put("profile", mockGuardQuery.activeGuardProfile().name());

        if (mockEnabled && realEnv) {
            details.put("status", "DJE operando em modo MOCK em ambiente real — publicações sem validade jurídica");
            Health.Builder builder = Health.status("OUT_OF_SERVICE");
            details.forEach(builder::withDetail);
            return builder.build();
        }
        Health.Builder builder = Health.up();
        details.forEach(builder::withDetail);
        return builder.build();
    }
}
