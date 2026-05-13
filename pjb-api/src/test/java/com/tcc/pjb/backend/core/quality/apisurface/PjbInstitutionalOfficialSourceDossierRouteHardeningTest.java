package com.tcc.pjb.backend.core.quality.apisurface;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PjbInstitutionalOfficialSourceDossierRouteHardeningTest {

    @Test
    void affiliationAndDelegatedControllersMustExposeOfficialSourceDossierThroughRouteRegistry() {
        String affiliation = ApiSurfaceTestSupport.read(Path.of("src/main/java/com/tcc/pjb/backend/controller/processual/comunicacao/institutional/affiliation/NationalCommunicationInstitutionalAffiliationController.java"));
        String delegated = ApiSurfaceTestSupport.read(Path.of("src/main/java/com/tcc/pjb/backend/controller/processual/comunicacao/institutional/affiliation/NationalCommunicationInstitutionalDelegatedOnboardingController.java"));
        assertTrue(affiliation.contains("@GetMapping(InstitutionalApiRoutes.PATH_AFFILIATION_OFFICIAL_SOURCE_DOSSIER)"));
        assertTrue(delegated.contains("@GetMapping(InstitutionalApiRoutes.PATH_DELEGATED_AFFILIATION_OFFICIAL_SOURCE_DOSSIER)"));
        assertFalse(affiliation.contains("@GetMapping(\"/afiliacoes/{affiliationId}/dossie-fontes-oficiais\")"));
        assertFalse(delegated.contains("@GetMapping(\"/adesoes-delegadas/{requestId}/dossie-fontes-oficiais\")"));
    }
}
