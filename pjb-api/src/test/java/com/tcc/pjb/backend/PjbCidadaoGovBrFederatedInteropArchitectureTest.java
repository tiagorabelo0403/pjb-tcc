package com.tcc.pjb.backend;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PjbCidadaoGovBrFederatedInteropArchitectureTest {

    @Test
    void deveManterSurfaceDeInteroperabilidadeFederadaDoCidadaoGovBr() throws Exception {
        Path controller = Path.of("src/main/java/com/tcc/pjb/backend/controller/cidadao/CidadaoGovBrInteroperabilidadeFederadaController.java");
        Path service = Path.of("src/main/java/com/tcc/pjb/backend/service/cidadao/govbr/CidadaoGovBrInteroperabilidadeFederadaService.java");
        Path response = Path.of("src/main/java/com/tcc/pjb/backend/model/dto/cidadao/govbr/CidadaoGovBrInteroperabilidadeFederadaResponse.java");
        Path accessRequest = Path.of("src/main/java/com/tcc/pjb/backend/model/dto/cidadao/govbr/CidadaoGovBrAcessoFederadoRequest.java");
        Path labels = Path.of("src/main/java/com/tcc/pjb/backend/service/cidadao/govbr/GovBrFederatedInteropLabels.java");

        assertTrue(Files.exists(controller));
        assertTrue(Files.exists(service));
        assertTrue(Files.exists(response));
        assertTrue(Files.exists(accessRequest));
        assertTrue(Files.exists(labels));

        String controllerSource = Files.readString(controller);
        assertTrue(controllerSource.contains("/interoperabilidade-federada"));
        assertTrue(controllerSource.contains("/acesso-federado/avaliar"));

        String serviceSource = Files.readString(service);
        assertTrue(serviceSource.contains("JudicialConnectorRegistry"));
        assertTrue(serviceSource.contains("JudicialConnectorSecurityPackService"));
        assertTrue(serviceSource.contains("JudicialConnectorRuntimePostureService"));
    }
}
