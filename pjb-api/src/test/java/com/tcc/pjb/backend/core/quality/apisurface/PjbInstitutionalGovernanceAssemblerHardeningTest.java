package com.tcc.pjb.backend.core.quality.apisurface;

import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry.NationalCommunicationInstitutionalSecureEntrySummaryResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalIntegrationSecurityPolicyResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalRecertificationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.topology.NationalCommunicationInstitutionalOrganizationBlueprintResponse;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalGovernanceAssemblerSupport;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalGovernanceSurfaceFacadeService;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PjbInstitutionalGovernanceAssemblerHardeningTest {

    @Test
    void governanceFacadeMustDelegateHeavyResponseAssemblyToGovernanceAssemblerSupport() {
        String facade = ApiSurfaceTestSupport.read(Path.of("src/main/java/com/tcc/pjb/backend/service/processual/comunicacao/institutional/governance/NationalCommunicationInstitutionalGovernanceSurfaceFacadeService.java"));

        assertTrue(facade.contains("NationalCommunicationInstitutionalGovernanceAssemblerSupport"));
        assertTrue(facade.contains("governanceAssemblerSupport.toResponse(summary)"));
        assertTrue(facade.contains("map(governanceAssemblerSupport::toResponse)"));
        assertFalse(facade.contains("new NationalCommunicationInstitutionalSecureEntrySummaryResponse("));
        assertFalse(facade.contains("new NationalCommunicationInstitutionalOrganizationBlueprintResponse("));
        assertFalse(facade.contains("new NationalCommunicationInstitutionalRecertificationResponse("));
        assertFalse(facade.contains("new NationalCommunicationInstitutionalIntegrationSecurityPolicyResponse("));
    }
}