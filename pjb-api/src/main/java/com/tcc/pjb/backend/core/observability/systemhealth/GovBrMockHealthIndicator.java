package com.tcc.pjb.backend.core.observability.systemhealth;

import com.tcc.pjb.backend.core.guard.MockGuardEnvironmentQuery;
import com.tcc.pjb.backend.integration.govbr.oidc.GovBrOidcProperties;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("govbr-mock")
public class GovBrMockHealthIndicator implements HealthIndicator {

    private final GovBrOidcProperties govBrProps;
    private final MockGuardEnvironmentQuery mockGuardQuery;

    public GovBrMockHealthIndicator(GovBrOidcProperties govBrProps, MockGuardEnvironmentQuery mockGuardQuery) {
        this.govBrProps = Objects.requireNonNull(govBrProps, "govBrProps");
        this.mockGuardQuery = Objects.requireNonNull(mockGuardQuery, "mockGuardQuery");
    }

    @Override
    public Health health() {
        boolean mockEnabled = govBrProps.mockEnabled();
        boolean realEnv = mockGuardQuery.isRealEnvironment();
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("mockEnabled", mockEnabled);
        details.put("realEnvironment", realEnv);
        details.put("profile", mockGuardQuery.activeGuardProfile().name());

        if (mockEnabled && realEnv) {
            details.put("status", "Gov.br OIDC em modo MOCK em ambiente real — autenticação de cidadão simulada");
            Health.Builder builder = Health.status("OUT_OF_SERVICE");
            details.forEach(builder::withDetail);
            return builder.build();
        }
        Health.Builder builder = Health.up();
        details.forEach(builder::withDetail);
        return builder.build();
    }
}
