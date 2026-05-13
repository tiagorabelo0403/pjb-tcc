package com.tcc.pjb.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PjbLegalAiProviderContractCoverageArchitectureTest {

    @Test
    void pactDaIaJuridicaDeveCobrirMinutaGroundingEConversationComPromocaoEGatesSoberanos() throws IOException {
        String pact = Files.readString(
                Path.of("src/test/resources/pacts/provider/PjbLegalAiConsumer-PjbLegalAiProvider.json"),
                StandardCharsets.UTF_8
        );

        assertThat(pact)
                .contains("/api/ai/legal/minuta")
                .contains("/api/ai/legal/grounding/check")
                .contains("/api/ai/legal/conversation")
                .contains("PROMOTED")
                .contains("STEP_UP_REQUIRED")
                .contains("BLOCKED")
                .contains("REVIEW_REQUIRED")
                .contains("AUTO_READONLY")
                .contains("HUMAN_REVIEW_REQUIRED")
                .contains("approvalStatus")
                .contains("trustZoneStatus")
                .contains("evidenceProvenanceTier")
                .contains("promotedDraftEvidenceIds")
                .contains("promotedGroundingEvidenceIds");
    }
}
