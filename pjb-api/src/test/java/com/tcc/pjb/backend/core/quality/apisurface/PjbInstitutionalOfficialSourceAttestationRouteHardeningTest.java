package com.tcc.pjb.backend.core.quality.apisurface;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PjbInstitutionalOfficialSourceAttestationRouteHardeningTest {

    @Test
    void institutionalControllersMustExposeAttestationAndRevalidationThroughRouteRegistry() {
        String affiliation = ApiSurfaceTestSupport.read(Path.of("src/main/java/com/tcc/pjb/backend/controller/processual/comunicacao/institutional/affiliation/NationalCommunicationInstitutionalAffiliationController.java"));
        String delegated = ApiSurfaceTestSupport.read(Path.of("src/main/java/com/tcc/pjb/backend/controller/processual/comunicacao/institutional/affiliation/NationalCommunicationInstitutionalDelegatedOnboardingController.java"));
        assertTrue(affiliation.contains("@GetMapping(InstitutionalApiRoutes.PATH_AFFILIATION_OFFICIAL_SOURCE_ATTESTATION)"));
        assertTrue(affiliation.contains("@GetMapping(InstitutionalApiRoutes.PATH_OFFICIAL_SOURCE_CONNECTORS)"));
        assertTrue(affiliation.contains("@PostMapping(InstitutionalApiRoutes.PATH_AFFILIATION_OFFICIAL_SOURCE_REVALIDATE)"));
        assertTrue(delegated.contains("@GetMapping(InstitutionalApiRoutes.PATH_DELEGATED_AFFILIATION_OFFICIAL_SOURCE_ATTESTATION)"));
        assertTrue(delegated.contains("@PostMapping(InstitutionalApiRoutes.PATH_DELEGATED_AFFILIATION_OFFICIAL_SOURCE_REVALIDATE)"));
        assertFalse(affiliation.contains("@GetMapping(\"/afiliacoes/{affiliationId}/atestacao-fontes-oficiais\")"));
        assertFalse(affiliation.contains("@GetMapping(\"/fontes-oficiais/conectores\")"));
        assertFalse(delegated.contains("@GetMapping(\"/adesoes-delegadas/{requestId}/atestacao-fontes-oficiais\")"));
    }
}
