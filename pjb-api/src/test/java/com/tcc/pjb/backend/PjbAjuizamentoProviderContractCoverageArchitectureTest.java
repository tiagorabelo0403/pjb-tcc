package com.tcc.pjb.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PjbAjuizamentoProviderContractCoverageArchitectureTest {

    @Test
    void pactDeAjuizamentoDeveCobrirRamosRoteamentoInferMapECapacidadesDeTribunal() throws IOException {
        String pact = Files.readString(
                Path.of("src/test/resources/pacts/provider/PjbAjuizamentoIntentConsumer-PjbAjuizamentoIntentProvider.json"),
                StandardCharsets.UTF_8
        );

        assertThat(pact)
                .contains("/api/v1/ai/ajuizamento/ramos")
                .contains("/api/v1/ai/ajuizamento/ramos/CIVIL")
                .contains("/api/v1/ai/ajuizamento/route")
                .contains("/api/v1/ai/ajuizamento/infer-map")
                .contains("/api/v1/ai/ajuizamento/catalog/tribunais/capabilities");
    }
}
