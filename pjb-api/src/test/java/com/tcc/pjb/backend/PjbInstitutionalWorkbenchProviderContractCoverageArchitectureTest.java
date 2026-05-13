package com.tcc.pjb.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PjbInstitutionalWorkbenchProviderContractCoverageArchitectureTest {

    @Test
    void pactDoInstitutionalWorkbenchDeveCobrirWorkspaceQuickActionsFilaEPreview() throws IOException {
        String pact = Files.readString(
                Path.of("src/test/resources/pacts/provider/PjbInstitutionalWorkbenchConsumer-PjbInstitutionalWorkbenchProvider.json"),
                StandardCharsets.UTF_8
        );

        assertThat(pact)
                .contains("/api/v1/institucional/workbench")
                .contains("/api/v1/institucional/workbench/quick-actions")
                .contains("/api/v1/institucional/workbench/operational-queue")
                .contains("/api/v1/institucional/workbench/action-preview");
    }
}
