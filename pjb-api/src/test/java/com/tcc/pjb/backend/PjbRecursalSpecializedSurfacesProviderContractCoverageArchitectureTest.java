package com.tcc.pjb.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PjbRecursalSpecializedSurfacesProviderContractCoverageArchitectureTest {

    @Test
    void pactDeSurfacesRecursaisEspecializadasDeveCobrirAdvogadoInstitucionalDocumentalEInteligencia() throws IOException {
        String pact = Files.readString(
                Path.of("src/test/resources/pacts/provider/PjbRecursalSpecializedSurfacesConsumer-PjbRecursalSpecializedSurfacesProvider.json"),
                StandardCharsets.UTF_8
        );

        assertThat(pact)
                .contains("/api/v1/processual/recursal/surfaces/attorney")
                .contains("/api/v1/processual/recursal/surfaces/institutional")
                .contains("/api/v1/processual/recursal/surfaces/documental")
                .contains("/api/v1/processual/recursal/surfaces/intelligence");
    }
}
