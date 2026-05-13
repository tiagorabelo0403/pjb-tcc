package com.tcc.pjb.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PjbLegalAiSignedOriginContractCoverageArchitectureTest {

    @Test
    void pactDaIaJuridicaDeveCobrirAtestacaoAssinadaValidaERejeicoesMateriaisNaBorda() throws IOException {
        String pact = Files.readString(
                Path.of("src/test/resources/pacts/provider/PjbLegalAiSignedOriginConsumer-PjbLegalAiSignedOriginProvider.json"),
                StandardCharsets.UTF_8
        );

        assertThat(pact)
                .contains("/api/ai/legal/minuta")
                .contains("/api/ai/legal/grounding/check")
                .contains("/api/ai/legal/conversation")
                .contains("X-PJB-Origin-Id")
                .contains("X-PJB-Body-Hash")
                .contains("X-PJB-Timestamp")
                .contains("X-PJB-Signature")
                .contains("SIGNED_ATTESTATION")
                .contains("signed_origin_signature_invalid")
                .contains("signed_origin_id_required")
                .contains("BODY_HASH_MISMATCH")
                .contains("401")
                .contains("409")
                .contains("200");
    }
}
