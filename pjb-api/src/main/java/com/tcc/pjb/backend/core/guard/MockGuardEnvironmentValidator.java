package com.tcc.pjb.backend.core.guard;

import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

public final class MockGuardEnvironmentValidator implements EnvironmentPostProcessor {

    public static final String REAL_ENV_PROPERTY   = "pjb.mock-guard.real-environment";
    public static final String HSM_MOCK_PROPERTY    = "pjb.hsm.mock-enabled";
    public static final String BNMP_MOCK_PROPERTY   = "pjb.bnmp.mock-enabled";
    public static final String DJE_MOCK_PROPERTY    = "pjb.dje.mock-enabled";
    public static final String GOVBR_MOCK_PROPERTY  = "pjb.integrations.govbr.mock-enabled";
    public static final String VECTOR_MOCK_PROPERTY = "pjb.ai.vector.mode";

    private static final Logger log = LoggerFactory.getLogger(MockGuardEnvironmentValidator.class);
    private static final Marker MOCK_GUARD_VIOLATION = MarkerFactory.getMarker("MOCK_GUARD_VIOLATION");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        MockGuardProfile detectedProfile = resolveWorstProfile(activeProfiles);

        if (!detectedProfile.isRealEnvironment()) {
            return;
        }

        checkMockEnabled(environment, HSM_MOCK_PROPERTY,    "true", "hsm",           detectedProfile);
        checkMockEnabled(environment, BNMP_MOCK_PROPERTY,   "true", "bnmp",          detectedProfile);
        checkMockEnabled(environment, DJE_MOCK_PROPERTY,    "true", "dje",           detectedProfile);
        checkMockEnabled(environment, GOVBR_MOCK_PROPERTY,  "true", "govbr",         detectedProfile);
        checkMockEnabled(environment, VECTOR_MOCK_PROPERTY, "mock", "vector-search", detectedProfile);

        environment.getPropertySources().addFirst(
                new MapPropertySource("mockGuardRealEnv", Map.of(REAL_ENV_PROPERTY, "true")));
    }

    private static void checkMockEnabled(ConfigurableEnvironment env,
                                          String property, String flagValue,
                                          String servico, MockGuardProfile profile) {
        String value = env.getProperty(property);
        if (value != null && value.equalsIgnoreCase(flagValue)) {
            MockGuardViolation violation = MockGuardViolation.of(servico, property, profile);
            log.error(MOCK_GUARD_VIOLATION,
                    "[MOCK-GUARD] {} em ambiente {}. Property: {}. ViolationId: {}",
                    violation.motivo(), profile.name(), property, violation.violationId());
            throw new MockGuardViolationException(violation);
        }
    }

    private static MockGuardProfile resolveWorstProfile(List<String> activeProfiles) {
        for (String p : activeProfiles) {
            MockGuardProfile candidate = MockGuardProfile.fromProfileName(p);
            if (candidate.isRealEnvironment()) {
                return candidate;
            }
        }
        return MockGuardProfile.DEV;
    }
}
