package com.tcc.pjb.backend.core.guard;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Arrays;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class MockGuardEnvironmentQuery {

    private static final Logger log = LoggerFactory.getLogger(MockGuardEnvironmentQuery.class);
    private static final Marker MOCK_GUARD_VIOLATION = MarkerFactory.getMarker("MOCK_GUARD_VIOLATION");

    private final Environment environment;
    private final MeterRegistry meterRegistry;

    public MockGuardEnvironmentQuery(Environment environment, MeterRegistry meterRegistry) {
        this.environment = Objects.requireNonNull(environment, "environment");
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry");
    }

    /**
     * Defense-in-depth contra mudanças pós-startup (refresh de config, bean criado tarde,
     * teste mal isolado). Em produção saudável, MockGuardEnvironmentValidator (EPP) barra
     * antes do contexto subir e este método nunca é executado em cenário de violação.
     */
    public boolean isRealEnvironment() {
        return "true".equalsIgnoreCase(environment.getProperty(MockGuardEnvironmentValidator.REAL_ENV_PROPERTY));
    }

    public void recordViolation(String service) {
        String activeProfile = resolveActiveProfile();
        log.error(MOCK_GUARD_VIOLATION,
                "[MOCK-GUARD] Tentativa de uso de mock em ambiente real detectada em runtime. servico={} profile={}",
                service, activeProfile);
        Counter.builder("pjb_mock_guard_violations_total")
                .tag("service", service)
                .tag("profile", activeProfile)
                .description("Violações de guard anti-mock em runtime")
                .register(meterRegistry)
                .increment();
    }

    public MockGuardProfile activeGuardProfile() {
        String[] profiles = environment.getActiveProfiles();
        return Arrays.stream(profiles)
                .map(MockGuardProfile::fromProfileName)
                .filter(MockGuardProfile::isRealEnvironment)
                .findFirst()
                .orElseGet(() -> profiles.length > 0
                        ? MockGuardProfile.fromProfileName(profiles[0])
                        : MockGuardProfile.DEV);
    }

    private String resolveActiveProfile() {
        return activeGuardProfile().name();
    }
}
