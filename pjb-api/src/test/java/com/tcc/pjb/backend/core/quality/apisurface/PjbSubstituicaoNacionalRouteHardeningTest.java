package com.tcc.pjb.backend.core.quality.apisurface;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PjbSubstituicaoNacionalRouteHardeningTest {

    @Test
    void processualSubstituicaoControllerMustUseCanonicalPlatformRoutes() {
        String controller = ApiSurfaceTestSupport.read(Path.of("src/main/java/com/tcc/pjb/backend/controller/processual/substituicao/arquitetura/PjbArquiteturaSubstituicaoNacionalController.java"));
        assertTrue(controller.contains("@RequestMapping(PjbSubstituicaoNacionalRoutes.CANONICAL_BASE)"));
        assertTrue(controller.contains("@GetMapping(PjbSubstituicaoNacionalRoutes.PATH_ARQUITETURA)"));
        assertTrue(controller.contains("@GetMapping(PjbSubstituicaoNacionalRoutes.PATH_CENTRO_COMANDO_TRIBUNAL)"));
        assertTrue(controller.contains("@GetMapping(PjbSubstituicaoNacionalRoutes.PATH_PRECEDENTES_QUALIFICADOS_TRIBUNAL)"));
        assertTrue(controller.contains("@GetMapping(PjbSubstituicaoNacionalRoutes.PATH_SUSTENTACAO)"));
        assertFalse(controller.contains("@GetMapping(\"/substituicao-nacional/arquitetura\")"));
        assertFalse(controller.contains("@RequestMapping(\"/api/v1/processual/plataforma\")"));
    }

    @Test
    void executionControllerMustUseCanonicalOperationalRoutes() {
        String controller = ApiSurfaceTestSupport.read(Path.of("src/main/java/com/tcc/pjb/backend/controller/processual/substituicao/nacional/PjbSubstituicaoNacionalExecutionController.java"));
        assertTrue(controller.contains("@RequestMapping(PjbSubstituicaoNacionalRoutes.CANONICAL_BASE)"));
        assertTrue(controller.contains("@GetMapping(PjbSubstituicaoNacionalRoutes.PATH_EXECUCAO_OPERACIONAL)"));
        assertTrue(controller.contains("@GetMapping(PjbSubstituicaoNacionalRoutes.PATH_COCKPIT)"));
        assertTrue(controller.contains("@GetMapping(PjbSubstituicaoNacionalRoutes.PATH_RECONCILIACAO_TRIBUNAL)"));
        assertTrue(controller.contains("@GetMapping(PjbSubstituicaoNacionalRoutes.PATH_EVIDENCIA_EXPORTAVEL_TRIBUNAL)"));
        assertFalse(controller.contains("@GetMapping(\"/substituicao-nacional/cockpit\")"));
        assertFalse(controller.contains("@GetMapping(\"/substituicao-nacional/reconciliacao/tribunal/{tribunalCodigo}\")"));
    }

    @Test
    void legadosControllerMustUseCanonicalLegacyRoutes() {
        String controller = ApiSurfaceTestSupport.read(Path.of("src/main/java/com/tcc/pjb/backend/controller/processual/substituicao/legados/PjbSubstituicaoLegadosController.java"));
        assertTrue(controller.contains("@RequestMapping(PjbSubstituicaoNacionalRoutes.CANONICAL_BASE)"));
        assertTrue(controller.contains("@GetMapping(PjbSubstituicaoNacionalRoutes.PATH_LEGADOS_PROCESSO)"));
        assertFalse(controller.contains("@RequestMapping(\"/api/v1/processual/plataforma\")"));
        assertFalse(controller.contains("@GetMapping(\"/{processoId}/substituicao-legados\")"));
    }

}
