package com.tcc.pjb.backend.core.quality.apisurface;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class PjbInstitutionalApiAliasCoverageTest {

    @Test
    void institutionalControllersMustExposeCanonicalBaseOnly() throws Exception {
        List<String> weak = ApiSurfaceTestSupport.controllerFiles().stream()
                .filter(path -> path.getFileName().toString().startsWith("NationalCommunicationInstitutional"))
                .filter(path -> !usesCanonicalBaseOnly(path))
                .map(Path::toString)
                .sorted()
                .toList();
        assertTrue(weak.isEmpty(), "Controllers institucionais fora da rota canônica única: " + weak);
    }

    @Test
    void institutionalDelegatedClosureControllerMustExposeOperatingModelEndpointThroughRouteRegistry() {
        String content = ApiSurfaceTestSupport.read(Path.of("src/main/java/com/tcc/pjb/backend/controller/processual/comunicacao/institutional/governance/NationalCommunicationInstitutionalDelegatedClosureController.java"));
        assertTrue(content.contains("@GetMapping(InstitutionalApiRoutes.PATH_MODELO_OPERACIONAL)"));
        assertTrue(content.contains("facadeService.modeloOperacional("));
    }

    @Test
    void institutionalAccessOrchestrationControllerMustExposeTrustGovernanceEndpointsThroughRouteRegistry() {
        String content = ApiSurfaceTestSupport.read(Path.of("src/main/java/com/tcc/pjb/backend/controller/processual/comunicacao/institutional/access/NationalCommunicationInstitutionalAccessOrchestrationController.java"));
        assertTrue(content.contains("@GetMapping(InstitutionalApiRoutes.PATH_DIMENSIONAMENTO_USUARIOS_INTERNOS)"));
        assertTrue(content.contains("@GetMapping(InstitutionalApiRoutes.PATH_GOVERNANCA_CONFIANCA)"));
        assertTrue(content.contains("@GetMapping(InstitutionalApiRoutes.PATH_PLANO_DADOS_HORIZONTAL)"));
        assertTrue(content.contains("@GetMapping(InstitutionalApiRoutes.PATH_PERFIL_OPERACIONAL)"));
        assertTrue(content.contains("@PostMapping(InstitutionalApiRoutes.PATH_GOVERNANCA_CONFIANCA_DECISOES)"));
    }

    private boolean usesCanonicalBaseOnly(Path path) {
        String content = ApiSurfaceTestSupport.read(path);
        return content.contains("@RequestMapping(InstitutionalApiRoutes.CANONICAL_BASE)")
                && !content.contains("LEGACY_BASE");
    }
}
