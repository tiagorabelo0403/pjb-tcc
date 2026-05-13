package com.tcc.pjb.backend.core.quality.apisurface;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PjbInstitutionalPanelSharedExperienceCoverageTest {

    @Test
    void panelProvisioningMustCoverSharedExperienceSurfaces() {
        String domain = ApiSurfaceTestSupport.read(Path.of("src/main/java/com/tcc/pjb/backend/core/comunicacao/institucional/panel/domain/InstitutionalPanelProvisioningReadiness.java"));
        String service = ApiSurfaceTestSupport.read(Path.of("src/main/java/com/tcc/pjb/backend/core/comunicacao/institucional/panel/application/InstitutionalPanelProvisioningReadinessApplicationService.java"))
                + ApiSurfaceTestSupport.read(Path.of("src/main/java/com/tcc/pjb/backend/core/comunicacao/institucional/panel/application/InstitutionalPanelProvisioningOutcomeFactory.java"));
        assertTrue(domain.contains("boolean calendarReady"));
        assertTrue(domain.contains("boolean hearingsReady"));
        assertTrue(domain.contains("boolean readingModeReady"));
        assertTrue(domain.contains("boolean triageReady"));
        assertTrue(domain.contains("boolean presentationReady"));
        assertTrue(domain.contains("boolean colorSystemReady"));
        assertTrue(domain.contains("boolean opinionFlowReady"));
        assertTrue(domain.contains("boolean calculatorReady"));
        assertTrue(domain.contains("InstitutionalHearingSchedulingGovernance hearingGovernance"));
        assertTrue(domain.contains("boolean sharedExperienceReady"));
        assertTrue(service.contains("CALENDAR_SURFACE_ROUTE"));
        assertTrue(service.contains("HEARING_SURFACE_ROUTE"));
        assertTrue(service.contains("READING_SURFACE_ROUTE"));
        assertTrue(service.contains("TRIAGE_SURFACE_ROUTE"));
        assertTrue(service.contains("CALCULATOR_SURFACE_ROUTE"));
        assertTrue(service.contains("hearingSchedulingGovernanceApplicationService"));
    }
}
