package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class NationalProceduralRoutingActionProfileSubphasesGovernanceTest {

    @Test
    void mustKeepActionProfileResolverAndPublicLawStageAsShortOrchestrators() throws Exception {
        String resolver = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralActionProfileResolver.java"));
        String publicLawResolver = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralActionProfilePublicLawResolver.java"));
        String specialProcedureResolver = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralActionProfileSpecialProcedureResolver.java"));
        String laborCriminalResolver = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralActionProfileLaborCriminalResolver.java"));
        String publicEntityResolver = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralActionProfilePublicEntityResolver.java"));

        assertTrue(resolver.contains("publicLawResolver.resolve(context).orElseGet(() -> privateRightsResolver.resolve(context))"));
        assertTrue(publicLawResolver.contains("specialProcedureResolver.resolve(context)"));
        assertTrue(publicLawResolver.contains(".or(() -> laborCriminalResolver.resolve(context))"));
        assertTrue(publicLawResolver.contains(".or(() -> publicEntityResolver.resolve(context))"));
        assertFalse(publicLawResolver.contains("containsAny(corpus, \"EXECUCAO FISCAL\""));
        assertFalse(publicLawResolver.contains("containsAny(corpus, \"CLT\""));
        assertFalse(publicLawResolver.contains("containsAny(corpus, \"BENEFICIO\""));
        assertTrue(specialProcedureResolver.contains("profile(\"MANDADO_SEGURANCA\""));
        assertTrue(laborCriminalResolver.contains("profile(\"RECLAMACAO_TRABALHISTA\""));
        assertTrue(publicEntityResolver.contains("String actionNature = switch (rito)"));
        assertTrue(publicEntityResolver.contains("appendPrevidenciarioChecklist(rito, reviewChecklist)"));
    }
}
