package com.tcc.pjb.backend.core.forum.routing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

class ForumDeskPortfolioProfileTest {

    @Test
    void shouldResolveExecutionDeskUsingConfiguredFallbackOrder() {
        ForumDeskPortfolioProfile profile = new ForumDeskPortfolioProfile(
                "TRIAGE",
                "GABINETE",
                "AUDIENCIA",
                "   ",
                "ESCALACAO",
                "ASSISTENTE",
                "COORDENACAO",
                "REDISTRIBUICAO",
                "DASH",
                List.of("BASE"),
                new LinkedHashMap<>()
        );

        assertThat(profile.executionDesk()).isEqualTo("AUDIENCIA");
    }

    @Test
    void toMapShouldDiscardNullMetadataAndExposeCoreFields() {
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("custom", "value");
        metadata.put("nullField", null);
        ForumDeskPortfolioProfile profile = new ForumDeskPortfolioProfile(
                "TRIAGE",
                "GABINETE",
                null,
                "CUMPRIMENTO",
                null,
                "ASSISTENTE",
                "COORDENACAO",
                "REDISTRIBUICAO",
                "DASH",
                List.of("BASE"),
                metadata
        );

        assertThat(profile.toMap())
                .containsEntry("triageDesk", "TRIAGE")
                .containsEntry("complianceDesk", "CUMPRIMENTO")
                .containsEntry("custom", "value")
                .doesNotContainKey("nullField");
    }
}
