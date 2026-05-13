package com.tcc.pjb.backend.core.quality.apisurface;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PjbInstitutionalInboxAndOperationalPanelRouteHardeningTest {

    @Test
    void institutionalFinalControllerMustUseRouteRegistryConstantsForInboxAndPanels() {
        String controller = ApiSurfaceTestSupport.read(Path.of("src/main/java/com/tcc/pjb/backend/controller/processual/comunicacao/institutional/panel/NationalCommunicationInstitutionalFinalController.java"));
        assertTrue(controller.contains("@GetMapping(InstitutionalApiRoutes.PATH_PANEL_ORGAO)"));
        assertTrue(controller.contains("@GetMapping(InstitutionalApiRoutes.PATH_FILAS_UNIDADE)"));
        assertTrue(controller.contains("@GetMapping(InstitutionalApiRoutes.PATH_AVISOS_EXTERNOS)"));
        assertTrue(controller.contains("@GetMapping(InstitutionalApiRoutes.PATH_PENDENTES_NAO_LEITURA)"));
        assertTrue(controller.contains("@PostMapping(InstitutionalApiRoutes.PATH_CERTIFICAR_NAO_LEITURA)"));
        assertFalse(controller.contains("@GetMapping(\"/painel-orgao\")"));
        assertFalse(controller.contains("@GetMapping(\"/filas-unidade\")"));
        assertFalse(controller.contains("@GetMapping(\"/avisos-externos\")"));
        assertFalse(controller.contains("@PostMapping(\"/certificar-nao-leitura\")"));
    }

    @Test
    void institutionalLifecycleControllerMustUseRouteRegistryConstantsForOperationalLifecycle() {
        String controller = ApiSurfaceTestSupport.read(Path.of("src/main/java/com/tcc/pjb/backend/controller/processual/comunicacao/institutional/operations/NationalCommunicationInstitutionalLifecycleController.java"));
        assertTrue(controller.contains("@GetMapping(InstitutionalApiRoutes.PATH_CADASTROS_OPERACIONAIS)"));
        assertTrue(controller.contains("@GetMapping(InstitutionalApiRoutes.PATH_CADASTRO_OPERACIONAL_AFILIACAO)"));
        assertTrue(controller.contains("@GetMapping(InstitutionalApiRoutes.PATH_CADASTRO_OPERACIONAL_SOLICITACAO)"));
        assertTrue(controller.contains("@GetMapping(InstitutionalApiRoutes.PATH_GUARDIAO_ENTRADA)"));
        assertFalse(controller.contains("@GetMapping(\"/cadastros-operacionais\")"));
        assertFalse(controller.contains("@GetMapping(\"/cadastros-operacionais/afiliacao/{affiliationId}\")"));
        assertFalse(controller.contains("@GetMapping(\"/cadastros-operacionais/solicitacao/{requestId}\")"));
        assertFalse(controller.contains("@GetMapping(\"/guardiao-entrada\")"));
    }
}
