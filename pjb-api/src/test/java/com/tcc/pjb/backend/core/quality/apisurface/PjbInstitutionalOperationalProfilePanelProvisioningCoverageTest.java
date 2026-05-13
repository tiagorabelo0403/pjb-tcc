package com.tcc.pjb.backend.core.quality.apisurface;

import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalOperationalProfileResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalPanelProvisioningReadinessResponse;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.support.NationalCommunicationInstitutionalFacadeSupport;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PjbInstitutionalOperationalProfilePanelProvisioningCoverageTest {

    @Test
    void operationalProfileResponseMustExposePanelProvisioningReadiness() {
        String response = ApiSurfaceTestSupport.read(Path.of("src/main/java/com/tcc/pjb/backend/model/dto/processual/comunicacao/institutional/operations/NationalCommunicationInstitutionalOperationalProfileResponse.java"));
        String support = ApiSurfaceTestSupport.read(Path.of("src/main/java/com/tcc/pjb/backend/service/processual/comunicacao/institutional/support/NationalCommunicationInstitutionalFacadeSupport.java"));

        assertTrue(response.contains("NationalCommunicationInstitutionalPanelProvisioningReadinessResponse panelProvisioning"));
        assertTrue(support.contains("InstitutionalPanelProvisioningReadinessApplicationService"));
        assertTrue(support.contains("toResponse(panelProvisioning)"));
        assertTrue(support.contains("item.calendarReady()"));
        assertTrue(support.contains("item.hearingsReady()"));
        assertTrue(support.contains("item.readingModeReady()"));
        assertTrue(support.contains("item.triageReady()"));
        assertTrue(support.contains("item.colorSystemReady()"));
    }
}