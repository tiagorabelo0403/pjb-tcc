package com.tcc.pjb.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PjbLegalAiSelectiveSignedOriginContractCoverageArchitectureTest {

    @Test
    void pactDaIaJuridicaDeveCobrirBrowserGovernadoECapabilitySensivelComCanalAssinado() throws IOException {
        String pact = Files.readString(
                Path.of("src/test/resources/pacts/provider/PjbLegalAiSelectiveSignedOriginConsumer-PjbLegalAiSelectiveSignedOriginProvider.json"),
                StandardCharsets.UTF_8
        );

        assertThat(pact)
                .contains("/api/ai/legal/conversation")
                .contains("LEGAL_GENERAL_ASSIST_V3")
                .contains("LEGAL_DRAFT_V2")
                .contains("X-PJB-Origin-Requirement")
                .contains("X-PJB-Origin-Capability")
                .contains("signed_attestation_required_for_capability")
                .contains("SIGNED_REQUIRED")
                .contains("BROWSER_OR_SIGNED")
                .contains("200")
                .contains("403");
    }
}
