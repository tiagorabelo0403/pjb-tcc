package com.tcc.pjb.backend.core.quality.apisurface;

import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalPanelCardResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalPanelChartResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalPanelNotificationResponse;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalPanelAssemblerSupport;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalPanelFacadeService;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PjbInstitutionalPanelAssemblerHardeningTest {

    @Test
    void panelFacadeMustUseDedicatedAssemblerSupport() {
        String facade = ApiSurfaceTestSupport.read(Path.of("src/main/java/com/tcc/pjb/backend/service/processual/comunicacao/institutional/panel/NationalCommunicationInstitutionalPanelFacadeService.java"));

        assertTrue(facade.contains("NationalCommunicationInstitutionalPanelAssemblerSupport"));
        assertTrue(facade.contains("assemblerSupport.toResponse(service.painelExecutivo"));
        assertTrue(facade.contains("map(assemblerSupport::toResponse)"));
        assertFalse(facade.contains("new NationalCommunicationInstitutionalPanelCardResponse("));
        assertFalse(facade.contains("new NationalCommunicationInstitutionalPanelNotificationResponse("));
        assertFalse(facade.contains("new NationalCommunicationInstitutionalPanelChartResponse("));
    }
}