package com.tcc.pjb.backend.core.quality.apisurface;

import com.tcc.pjb.backend.service.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalGovernanceAssemblerSupport;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalGovernanceSurfaceFacadeService;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.state.NationalCommunicationInstitutionalStateBundleFacadeService;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.support.NationalCommunicationInstitutionalFacadeSupport;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.surface.NationalCommunicationInstitutionalSurfaceAssemblerSupport;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.surface.NationalCommunicationInstitutionalSurfaceFacadeService;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PjbInstitutionalFacadeSpineHardeningTest {

    @Test
    void institutionalFacadesMustUseSharedSupportAndStateBundleSpine() {
        String governance = ApiSurfaceTestSupport.read(Path.of("src/main/java/com/tcc/pjb/backend/service/processual/comunicacao/institutional/governance/NationalCommunicationInstitutionalGovernanceSurfaceFacadeService.java"));
        String surface = ApiSurfaceTestSupport.read(Path.of("src/main/java/com/tcc/pjb/backend/service/processual/comunicacao/institutional/surface/NationalCommunicationInstitutionalSurfaceFacadeService.java"));
        String filter = ApiSurfaceTestSupport.read(Path.of("src/main/java/com/tcc/pjb/backend/configs/datasource/PjbInstitutionalDataPlaneFilter.java"));

        assertTrue(governance.contains("NationalCommunicationInstitutionalFacadeSupport"));
        assertTrue(governance.contains("NationalCommunicationInstitutionalStateBundleFacadeService"));
        assertTrue(governance.contains("NationalCommunicationInstitutionalGovernanceAssemblerSupport"));
        assertTrue(governance.contains("stateBundleFacadeService.carregar(affiliationId, nominationId)"));
        assertTrue(surface.contains("NationalCommunicationInstitutionalFacadeSupport"));
        assertTrue(surface.contains("NationalCommunicationInstitutionalStateBundleFacadeService"));
        assertTrue(surface.contains("NationalCommunicationInstitutionalSurfaceAssemblerSupport"));
        assertTrue(surface.contains("stateBundleFacadeService.carregar(summary)"));
        assertFalse(surface.contains("new NationalCommunicationInstitutionalStateBundle"));
        assertTrue(filter.contains("NationalCommunicationInstitutionalStateBundleFacadeService"));
        assertFalse(filter.contains("InstitutionalHorizontalDataPlaneApplicationService"));
        assertFalse(filter.contains("InstitutionalOperationalProfileProjectionApplicationService"));
        assertFalse(filter.contains("InstitutionalEntryActivationDecisionApplicationService"));
    }
}