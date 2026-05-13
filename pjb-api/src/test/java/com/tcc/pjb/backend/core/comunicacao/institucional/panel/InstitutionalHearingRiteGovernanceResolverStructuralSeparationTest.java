package com.tcc.pjb.backend.core.comunicacao.institucional.panel;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class InstitutionalHearingRiteGovernanceResolverStructuralSeparationTest {

    private static final Path RESOLVER = Path.of("src/main/java/com/tcc/pjb/backend/core/comunicacao/institucional/panel/application/InstitutionalHearingRiteGovernanceResolver.java");
    private static final Path CONTEXT_FACTORY = Path.of("src/main/java/com/tcc/pjb/backend/core/comunicacao/institucional/panel/application/InstitutionalHearingRiteGovernanceContextFactory.java");
    private static final Path FACTORY = Path.of("src/main/java/com/tcc/pjb/backend/core/comunicacao/institucional/panel/application/InstitutionalHearingRiteGovernanceFactory.java");

    @Test
    void mustKeepRiteGovernanceResolverAsShortOrchestrator() throws Exception {
        String source = Files.readString(RESOLVER, StandardCharsets.UTF_8);
        assertTrue(source.contains("InstitutionalHearingRiteGovernanceContextFactory"));
        assertTrue(source.contains("InstitutionalHearingCivilAndJuizadosRiteResolver"));
        assertTrue(source.contains("InstitutionalHearingPublicProtectionAndPenalRiteResolver"));
        assertTrue(source.contains("InstitutionalHearingSpecializedJusticeRiteResolver"));
        assertTrue(source.contains("InstitutionalHearingRecursalRiteResolver"));
        assertFalse(source.contains("private InstitutionalHearingRiteGovernance buildRite("));
        assertFalse(source.contains("boolean civilBroad ="));
        assertFalse(source.contains("addIfRelevant(rites, buildRite("));
    }

    @Test
    void mustKeepContextFactoryAsSinglePlaceForScopeAndCejuscDerivation() throws Exception {
        String source = Files.readString(CONTEXT_FACTORY, StandardCharsets.UTF_8);
        assertTrue(source.contains("processProfile == InstitutionalProcessProfile.CONCILIADOR"));
        assertTrue(source.contains("processProfile == InstitutionalProcessProfile.MEDIADOR"));
        assertTrue(source.contains("processProfile == InstitutionalProcessProfile.AGENDADOR_CONCILIACAO"));
        assertTrue(source.contains("boolean militaryFederalScope = militaryScope && scopeSupport.scopeMatches(scope, \"FED\")"));
    }

    @Test
    void mustKeepBuildRiteCentralizedInFactory() throws Exception {
        String source = Files.readString(FACTORY, StandardCharsets.UTF_8);
        assertTrue(source.contains("InstitutionalHearingRiteGovernance buildRite("));
        assertTrue(source.contains("List<String> mergeSegregationGuards"));
        assertFalse(source.contains("class InstitutionalHearingRiteGovernanceResolver"));
    }
}
