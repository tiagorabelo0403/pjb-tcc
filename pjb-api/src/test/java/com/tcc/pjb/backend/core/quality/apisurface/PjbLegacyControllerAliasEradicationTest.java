package com.tcc.pjb.backend.core.quality.apisurface;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PjbLegacyControllerAliasEradicationTest {

    private static final String LEGACY_PREFIX = "/com/tcc/pjb/backend/api";
    private static final Set<String> APPROVED_COMPATIBILITY_FILES = Set.of(
            "src/main/java/com/tcc/pjb/backend/modules/advocacia/controller/LegacyAdvocaciaClienteForwardController.java"
    );

    @Test
    void runtimeControllersMustNotExposeLegacyAliasesOutsideCuratedCompatibilityIngress() throws Exception {
        var offenders = ApiSurfaceTestSupport.controllerFiles().stream()
                .filter(path -> !APPROVED_COMPATIBILITY_FILES.contains(normalizePath(path)))
                .filter(path -> ApiSurfaceTestSupport.read(path).contains(LEGACY_PREFIX))
                .map(PjbLegacyControllerAliasEradicationTest::normalizePath)
                .toList();

        assertTrue(offenders.isEmpty(), "Controllers ainda expondo alias legado: " + offenders);
    }

    @Test
    void compatibilityIngressMustStayExplicitlyGatedByLegacyMode() {
        String content = ApiSurfaceTestSupport.read(Path.of(
                "src/main/java/com/tcc/pjb/backend/modules/advocacia/controller/LegacyAdvocaciaClienteForwardController.java"));

        assertTrue(content.contains("@ConditionalOnProperty(prefix = \"pjb.api.legacy-paths\", name = \"enabled\", havingValue = \"true\")"));
        assertTrue(content.contains("@RequestMapping(\"/com/tcc/pjb/backend/api/modulos/advocacia/clientes\")"));
    }

    @Test
    void routeGovernanceMustUseCanonicalChatAndUsuarioSurfacesOnly() {
        String content = ApiSurfaceTestSupport.applicationYaml();

        assertTrue(content.contains("paths: [\"/api/v1/usuarios/**\"]"));
        assertTrue(content.contains("paths: [\"/api/v1/chat/**\"]"));
        assertTrue(!content.contains("/com/tcc/pjb/backend/api/usuarios/**"));
        assertTrue(!content.contains("/com/tcc/pjb/backend/api/chat/**"));
    }

    private static String normalizePath(Path path) {
        return path.toString().replace('\\', '/');
    }
}
