package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.core.processo.juizado.procedural.NationalProceduralJuizadoDecision;
import com.tcc.pjb.backend.core.processo.juizado.procedural.NationalProceduralJuizadoDecisionResolver;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class NationalProceduralRoutingJuizadoDecisionSubphasesGovernanceTest {

    @Test
    void mustKeepJuizadoDecisionAndTrackStagesAsShortOrchestrators() throws Exception {
        String resolver = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/processo/juizado/procedural/NationalProceduralJuizadoDecisionResolver.java"));
        String exclusionResolver = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/processo/juizado/procedural/NationalProceduralJuizadoExclusionResolver.java"));
        String trackResolver = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/processo/juizado/procedural/NationalProceduralJuizadoTrackResolver.java"));
        String classifier = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/processo/juizado/procedural/NationalProceduralJuizadoTrackClassifier.java"));
        String federalTrackResolver = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/processo/juizado/procedural/NationalProceduralJuizadoFederalTrackResolver.java"));
        String civelTrackResolver = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/processo/juizado/procedural/NationalProceduralJuizadoCivelTrackResolver.java"));

        assertTrue(resolver.contains("exclusionResolver.resolve(context).orElseGet(() -> trackResolver.resolve(context))"));
        assertTrue(trackResolver.contains("switch (classifier.classify(context))"));
        assertTrue(trackResolver.contains("case FEDERAL -> federalTrackResolver.resolve(context);"));
        assertTrue(trackResolver.contains("case CIVEL -> civelTrackResolver.resolve(context);"));
        assertFalse(trackResolver.contains("valor.compareTo(ZERO) <= 0"));
        assertFalse(trackResolver.contains("containsAny(actionProfile.actionNature(), \"OBRIGACAO_DE_FAZER\""));
        assertTrue(exclusionResolver.contains("messages.excludedBySpecialNatureAlert()"));
        assertTrue(classifier.contains("return NationalProceduralJuizadoTrackLane.CRIMINAL;"));
        assertTrue(federalTrackResolver.contains("messages.federalJuizadoComplexEvidenceAlert()"));
        assertTrue(civelTrackResolver.contains("messages.civelJuizadoComplexEvidenceAlert()"));
    }
}
