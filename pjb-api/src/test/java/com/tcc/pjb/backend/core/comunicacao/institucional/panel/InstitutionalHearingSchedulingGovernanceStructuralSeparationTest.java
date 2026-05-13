package com.tcc.pjb.backend.core.comunicacao.institucional.panel;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class InstitutionalHearingSchedulingGovernanceStructuralSeparationTest {

    private static final Path SERVICE = Path.of("src/main/java/com/tcc/pjb/backend/core/comunicacao/institucional/panel/application/InstitutionalHearingSchedulingGovernanceApplicationService.java");
    private static final Path SCOPE = Path.of("src/main/java/com/tcc/pjb/backend/core/comunicacao/institucional/panel/application/InstitutionalHearingSchedulingScopeSupport.java");

    @Test
    void mustKeepGovernanceApplicationServiceAsShortOrchestrator() throws Exception {
        String source = Files.readString(SERVICE, StandardCharsets.UTF_8);
        assertTrue(source.contains("InstitutionalHearingSchedulingCapabilityResolver"));
        assertTrue(source.contains("InstitutionalHearingSchedulingScopeSupport"));
        assertTrue(source.contains("InstitutionalHearingRiteGovernanceResolver"));
        assertFalse(source.contains("private List<InstitutionalHearingRiteGovernance> buildRiteGovernances"));
        assertFalse(source.contains("private List<String> resolveOperationalQueues"));
        assertFalse(source.contains("private List<String> resolveSegregationGuards"));
        assertFalse(source.contains("private LinkedHashSet<String> requestActors"));
    }

    @Test
    void mustKeepScopeSupportWithExpandedFirstNonBlankOverloads() throws Exception {
        String source = Files.readString(SCOPE, StandardCharsets.UTF_8);
        assertTrue(source.contains("firstNonBlank(String first, String second, String third, String fourth)"));
        assertTrue(source.contains("firstNonBlank(String first, String second, String third, String fourth, String fifth)"));
    }
}
