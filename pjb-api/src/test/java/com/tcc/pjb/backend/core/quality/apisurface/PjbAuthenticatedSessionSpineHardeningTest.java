package com.tcc.pjb.backend.core.quality.apisurface;

import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry.NationalCommunicationInstitutionalEntrySummaryResponse;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.state.NationalCommunicationInstitutionalStateBundleFacadeService;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.surface.NationalCommunicationInstitutionalSurfaceFacadeService;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PjbAuthenticatedSessionSpineHardeningTest {

    @Test
    void authenticatedSessionMustBeSharedByInstitutionalEntryAndSecurityContext() {
        String surface = ApiSurfaceTestSupport.read(Path.of("src/main/java/com/tcc/pjb/backend/service/processual/comunicacao/institutional/surface/NationalCommunicationInstitutionalSurfaceFacadeService.java"));
        String securityContext = ApiSurfaceTestSupport.read(Path.of("src/main/java/com/tcc/pjb/backend/service/security/surface/SecurityContextSurfaceFacadeService.java"));
        String sessionFacade = ApiSurfaceTestSupport.read(Path.of("src/main/java/com/tcc/pjb/backend/service/security/context/PjbAuthenticatedSessionFacadeService.java"));
        String entryResponse = ApiSurfaceTestSupport.read(Path.of("src/main/java/com/tcc/pjb/backend/model/dto/processual/comunicacao/institutional/entry/NationalCommunicationInstitutionalEntrySummaryResponse.java"));
        String securityResponse = ApiSurfaceTestSupport.read(Path.of("src/main/java/com/tcc/pjb/backend/model/dto/security/context/SecurityContextResponse.java"));

        assertTrue(surface.contains("PjbAuthenticatedSessionFacadeService"));
        assertTrue(surface.contains("authenticatedSessionFacadeService.atual(summary, stateBundle)"));
        assertTrue(securityContext.contains("PjbAuthenticatedSessionFacadeService"));
        assertTrue(securityContext.contains("authenticatedSessionFacadeService.atual()"));
        assertTrue(sessionFacade.contains("CurrentAuthenticationContextService"));
        assertTrue(sessionFacade.contains("GovBrIdentityAssuranceApplicationService"));
        assertTrue(sessionFacade.contains("NationalCommunicationInstitutionalStateBundleFacadeService"));
        assertTrue(entryResponse.contains("PjbAuthenticatedSessionResponse"));
        assertTrue(entryResponse.contains("sessaoAutenticada"));
        assertTrue(securityResponse.contains("PjbAuthenticatedSessionResponse institutionalSession"));
    }
}