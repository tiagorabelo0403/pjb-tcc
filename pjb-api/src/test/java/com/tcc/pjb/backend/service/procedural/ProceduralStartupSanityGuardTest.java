package com.tcc.pjb.backend.service.procedural;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.service.procedural.ProceduralArchitectureSanityService.SanityReport;
import com.tcc.pjb.backend.service.procedural.ProceduralLegacyBoundaryAuditService.BoundaryReport;
import com.tcc.pjb.backend.service.procedural.ProceduralLegacyBoundaryAuditService.BoundaryViolation;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.context.ApplicationContextException;
import org.springframework.core.env.Environment;

class ProceduralStartupSanityGuardTest {

    @Test
    void doesNotFailWhenHealthy() {
        ProceduralArchitectureSanityService architecture = mock(ProceduralArchitectureSanityService.class);
        ProceduralLegacyBoundaryAuditService boundary = mock(ProceduralLegacyBoundaryAuditService.class);
        Environment environment = mock(Environment.class);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"local"});
        when(architecture.report()).thenReturn(new SanityReport(
                Instant.now(),
                true,
                10,
                10,
                10,
                10,
                List.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of()
        ));
        when(boundary.report()).thenReturn(new BoundaryReport(Instant.now(), true, true, 10, List.of("src/main/java"), List.of()));

        ProceduralBootstrapGovernanceProperties properties = new ProceduralBootstrapGovernanceProperties();
        ProceduralStartupSanityGuard guard = new ProceduralStartupSanityGuard(architecture, boundary, properties, environment);

        assertDoesNotThrow(() -> guard.run(new DefaultApplicationArguments(new String[0])));
    }

    @Test
    void failsFastOnProdWhenArchitectureIsUnhealthy() {
        ProceduralArchitectureSanityService architecture = mock(ProceduralArchitectureSanityService.class);
        ProceduralLegacyBoundaryAuditService boundary = mock(ProceduralLegacyBoundaryAuditService.class);
        Environment environment = mock(Environment.class);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(architecture.report()).thenReturn(new SanityReport(
                Instant.now(),
                false,
                10,
                10,
                10,
                10,
                List.of("workflow_blueprint_incomplete"),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of()
        ));
        when(boundary.report()).thenReturn(new BoundaryReport(Instant.now(), true, true, 10, List.of("src/main/java"), List.of()));

        ProceduralBootstrapGovernanceProperties properties = new ProceduralBootstrapGovernanceProperties();
        ProceduralStartupSanityGuard guard = new ProceduralStartupSanityGuard(architecture, boundary, properties, environment);

        assertThrows(ApplicationContextException.class, () -> guard.run(new DefaultApplicationArguments(new String[0])));
    }

    @Test
    void failsFastWhenLegacyBoundaryHasViolationsAndValidationIsEnabled() {
        ProceduralArchitectureSanityService architecture = mock(ProceduralArchitectureSanityService.class);
        ProceduralLegacyBoundaryAuditService boundary = mock(ProceduralLegacyBoundaryAuditService.class);
        Environment environment = mock(Environment.class);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"local"});
        when(architecture.report()).thenReturn(new SanityReport(
                Instant.now(),
                true,
                10,
                10,
                10,
                10,
                List.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of()
        ));
        when(boundary.report()).thenReturn(new BoundaryReport(
                Instant.now(),
                true,
                false,
                10,
                List.of("src/main/java"),
                List.of(new BoundaryViolation("core/preflight/Example.java", "com.tcc.pjb.backend.core.preflight", "uso_direto_enum_legado_em_camada_operacional", List.of(2, 3), true, true))
        ));

        ProceduralBootstrapGovernanceProperties properties = new ProceduralBootstrapGovernanceProperties();
        properties.setFailFast(true);
        ProceduralStartupSanityGuard guard = new ProceduralStartupSanityGuard(architecture, boundary, properties, environment);

        assertThrows(ApplicationContextException.class, () -> guard.run(new DefaultApplicationArguments(new String[0])));
    }
}
