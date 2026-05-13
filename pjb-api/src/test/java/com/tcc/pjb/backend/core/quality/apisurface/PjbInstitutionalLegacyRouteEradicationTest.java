package com.tcc.pjb.backend.core.quality.apisurface;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class PjbInstitutionalLegacyRouteEradicationTest {

    private static final String LEGACY_ROUTE = "/api/v1/processual" + "/comunicacoes/institucional";
    private static final Set<String> ALLOWED_REFERENCES = Set.of(
            "src/test/java/com/tcc/pjb/backend/configs/datasource/PjbInstitutionalDataPlaneFilterTest.java",
            "src/test/java/com/tcc/pjb/backend/core/quality/apisurface/PjbInstitutionalCanonicalRouteReferenceTest.java",
            "src/test/java/com/tcc/pjb/backend/core/quality/apisurface/PjbInstitutionalRouteGovernanceCoverageTest.java");

    @Test
    void legacyRouteMustRemainOnlyInNegativeTests() throws Exception {
        try (Stream<Path> stream = Files.walk(Path.of("."))) {
            List<String> files = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> !path.toString().contains("/target/"))
                    .filter(path -> path.toString().endsWith(".java") || path.toString().endsWith(".yml") || path.toString().endsWith(".yaml") || path.toString().endsWith(".md") || path.toString().endsWith(".json"))
                    .filter(path -> ApiSurfaceTestSupport.read(path).contains(LEGACY_ROUTE))
                    .map(path -> path.toString().replace('\\', '/').replaceFirst("^\\./", ""))
                    .sorted()
                    .toList();
            assertEquals(ALLOWED_REFERENCES.stream().sorted().toList(), files, "Rota institucional legada deve sobreviver apenas em testes negativos de endurecimento.");
        }
    }
}
