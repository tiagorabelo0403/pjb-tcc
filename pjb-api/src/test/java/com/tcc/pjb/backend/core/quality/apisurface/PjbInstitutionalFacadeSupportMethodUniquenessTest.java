package com.tcc.pjb.backend.core.quality.apisurface;

import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalPanelProvisioningReadinessResponse;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.support.NationalCommunicationInstitutionalFacadeSupport;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PjbInstitutionalFacadeSupportMethodUniquenessTest {

    @Test
    void facadeSupportMustExposeSinglePanelProvisioningResponseMapper() {
        String support = ApiSurfaceTestSupport.read(Path.of("src/main/java/com/tcc/pjb/backend/service/processual/comunicacao/institutional/support/NationalCommunicationInstitutionalFacadeSupport.java"));
        String signature = "public NationalCommunicationInstitutionalPanelProvisioningReadinessResponse toResponse(InstitutionalPanelProvisioningReadiness item)";
        int count = support.split(java.util.regex.Pattern.quote(signature), -1).length - 1;
        assertEquals(1, count);
    }
}