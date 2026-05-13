package com.tcc.pjb.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PjbLegalAiEdgePolicyContractCoverageArchitectureTest {

    @Test
    void pactDaBordaDaIaJuridicaDeveCobrirOrigemNaoAtestadaEContentTypeInvalidoNasTresRotas() throws IOException {
        String pact = Files.readString(
                Path.of("src/test/resources/pacts/provider/PjbLegalAiEdgePolicyConsumer-PjbLegalAiEdgePolicyProvider.json"),
                StandardCharsets.UTF_8
        );

        assertThat(pact)
                .contains("/api/ai/legal/minuta")
                .contains("/api/ai/legal/grounding/check")
                .contains("/api/ai/legal/conversation")
                .contains("browser_origin_not_allowed")
                .contains("content_type_not_allowed")
                .contains("application/json")
                .contains("text/plain")
                .contains("403")
                .contains("415");
    }
}
