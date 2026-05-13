package com.tcc.pjb.backend.core.quality.apisurface;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PjbJudgeDelegationAndObservabilityRouteHardeningTest {

    @Test
    void judgeDelegationControllerMustUseCanonicalDelegationRoutes() {
        String controller = ApiSurfaceTestSupport.read(Path.of("src/main/java/com/tcc/pjb/backend/controller/JudgeDelegationController.java"));
        assertTrue(controller.contains("@RequestMapping(JudgeDelegationApiRoutes.CANONICAL_BASE)"));
        assertTrue(controller.contains("@PostMapping(JudgeDelegationApiRoutes.PATH_ISSUE)"));
        assertTrue(controller.contains("@PostMapping(JudgeDelegationApiRoutes.PATH_REQUEST_APPROVE)"));
        assertTrue(controller.contains("@GetMapping(JudgeDelegationApiRoutes.PATH_REQUESTS_PENDING)"));
        assertTrue(controller.contains("@PostMapping(JudgeDelegationApiRoutes.PATH_VERIFY)"));
        assertFalse(controller.contains("@RequestMapping(\"/api/v1/judge/delegation\")"));
    }

    @Test
    void nationalObservabilityControllerMustUseCanonicalObservabilityRoutes() {
        String controller = ApiSurfaceTestSupport.read(Path.of("src/main/java/com/tcc/pjb/backend/controller/admin/NationalObservabilityController.java"));
        assertTrue(controller.contains("@RequestMapping(NationalObservabilityRoutes.CANONICAL_BASE)"));
        assertTrue(controller.contains("@GetMapping(NationalObservabilityRoutes.PATH_SUBSTITUICAO_READINESS)"));
        assertTrue(controller.contains("@GetMapping(NationalObservabilityRoutes.PATH_SUBSTITUICAO_WAR_ROOM_TRIBUNAL)"));
        assertTrue(controller.contains("@GetMapping(NationalObservabilityRoutes.PATH_SUBSTITUICAO_CUTOVER_MATRIX_TRIBUNAL)"));
        assertTrue(controller.contains("@GetMapping(NationalObservabilityRoutes.PATH_PLATAFORMA_SUSTENTACAO)"));
        assertFalse(controller.contains("@GetMapping(\"/substituicao-war-room/tribunal/{tribunalCodigo}\")"));
    }
}
