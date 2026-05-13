package com.tcc.pjb.backend;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PjbCidadaoGovBrAcervoSurfaceArchitectureTest {

    @Test
    void deveManterSurfaceSoberanaDoAcervoGovBrDoCidadao() throws Exception {
        Path controller = Path.of("src/main/java/com/tcc/pjb/backend/controller/cidadao/CidadaoGovBrAcervoUnificadoController.java");
        Path service = Path.of("src/main/java/com/tcc/pjb/backend/service/cidadao/govbr/CidadaoGovBrAcervoUnificadoService.java");
        Path dto = Path.of("src/main/java/com/tcc/pjb/backend/model/dto/cidadao/govbr/CidadaoGovBrAcervoUnificadoResponse.java");
        Path labels = Path.of("src/main/java/com/tcc/pjb/backend/service/cidadao/govbr/GovBrCitizenPanelLabels.java");

        assertTrue(Files.exists(controller));
        assertTrue(Files.exists(service));
        assertTrue(Files.exists(dto));
        assertTrue(Files.exists(labels));

        String controllerSource = Files.readString(controller);
        assertTrue(controllerSource.contains("/api/v1/cidadao/govbr"));
        assertTrue(controllerSource.contains("/acervo-unificado"));

        String serviceSource = Files.readString(service);
        assertTrue(serviceSource.contains("CPF_CANONICO_MULTISSISTEMA"));
        assertTrue(serviceSource.contains("listVisibleCurrentUser"));
    }
}
