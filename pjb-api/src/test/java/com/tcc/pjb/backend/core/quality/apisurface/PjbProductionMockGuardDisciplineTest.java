package com.tcc.pjb.backend.core.quality.apisurface;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class PjbProductionMockGuardDisciplineTest {

    private static final Pattern DEV_TEST_DEMO_PROFILE = Pattern.compile(
            "@Profile\\s*\\(\\s*\\{\\s*\"dev\"\\s*,\\s*\"test\"\\s*,\\s*\"demo\"\\s*}\\s*\\)");

    @Test
    void executableMocksAndDemoSeedsInMainMustBeProfileGuarded() throws Exception {
        for (Path path : List.of(
                Path.of("src/main/java/com/tcc/pjb/backend/integration/govbr/mock/GovBrMockSignatureService.java"),
                Path.of("src/main/java/com/tcc/pjb/backend/demo/PjbDemoDataInitializer.java"))) {
            String content = ApiSurfaceTestSupport.read(path);

            assertTrue(content.contains("org.springframework.context.annotation.Profile"),
                    path + " deve declarar Profile explicitamente.");
            assertTrue(DEV_TEST_DEMO_PROFILE.matcher(content).find(),
                    path + " deve ficar restrito aos perfis dev/test/demo.");
        }
    }

    @Test
    void demoHttpSurfaceMustNotBePermitAll() throws Exception {
        String securityConfig = ApiSurfaceTestSupport.read(
                Path.of("src/main/java/com/tcc/pjb/backend/configs/SecurityConfig.java"));
        String controller = ApiSurfaceTestSupport.read(
                Path.of("src/main/java/com/tcc/pjb/backend/controller/demo/PjbDemoStatusController.java"));

        assertFalse(securityConfig.contains("requestMatchers(\"/demo/**\").permitAll()"),
                "/demo/** nao deve ficar publico por SecurityFilterChain.");
        assertFalse(controller.contains("@PreAuthorize(\"permitAll()\")"),
                "PjbDemoStatusController nao deve declarar permitAll.");
        assertTrue(controller.contains("hasAnyAuthority('ROLE_ADMIN','ROLE_ADMINISTRADOR')"),
                "PjbDemoStatusController deve exigir autoridade administrativa.");
    }
}
