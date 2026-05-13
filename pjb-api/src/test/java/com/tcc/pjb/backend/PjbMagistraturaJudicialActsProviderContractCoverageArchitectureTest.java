package com.tcc.pjb.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PjbMagistraturaJudicialActsProviderContractCoverageArchitectureTest {

    @Test
    void pactDeAtosDaMagistraturaDeveCobrirWorkspacePreviewBloqueioAutomationPreviewEExecucoesMultigrau() throws IOException {
        String pact = Files.readString(
                Path.of("src/test/resources/pacts/provider/PjbMagistraturaActsConsumer-PjbMagistraturaActsProvider.json"),
                StandardCharsets.UTF_8
        );

        assertThat(pact)
                .contains("/api/v1/magistratura/atos")
                .contains("/api/v1/magistratura/processos/9001/atos/preview")
                .contains("/api/v1/magistratura/processos/9001/atos/automation-preview")
                .contains("/api/v1/magistratura/processos/9001/atos")
                .contains("action=SENTENCA")
                .contains("VOTO_COLEGIADO")
                .contains("DECISAO_PLENARIA");
    }
}
