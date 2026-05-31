package com.tcc.pjb.backend.core.observability.systemhealth;

import com.tcc.pjb.backend.core.guard.MockGuardEnvironmentQuery;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component("vector-search-mock")
public class VectorSearchMockHealthIndicator implements HealthIndicator {

    private static final String VECTOR_MODE_PROPERTY = "pjb.ai.vector.mode";

    private final Environment environment;
    private final MockGuardEnvironmentQuery mockGuardQuery;

    public VectorSearchMockHealthIndicator(Environment environment,
                                           MockGuardEnvironmentQuery mockGuardQuery) {
        this.environment = Objects.requireNonNull(environment, "environment");
        this.mockGuardQuery = Objects.requireNonNull(mockGuardQuery, "mockGuardQuery");
    }

    @Override
    public Health health() {
        String mode = environment.getProperty(VECTOR_MODE_PROPERTY, "disabled");
        boolean isMock = "mock".equalsIgnoreCase(mode);
        boolean realEnv = mockGuardQuery.isRealEnvironment();
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("mode", mode);
        details.put("realEnvironment", realEnv);
        details.put("profile", mockGuardQuery.activeGuardProfile().name());

        if (isMock && realEnv) {
            details.put("status", "VectorSearch em modo MOCK em ambiente real — busca semântica retorna dados fictícios");
            Health.Builder builder = Health.status("OUT_OF_SERVICE");
            details.forEach(builder::withDetail);
            return builder.build();
        }
        Health.Builder builder = Health.up();
        details.forEach(builder::withDetail);
        return builder.build();
    }
}
