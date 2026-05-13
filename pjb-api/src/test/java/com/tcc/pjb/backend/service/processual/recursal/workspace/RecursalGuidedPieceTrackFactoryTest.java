package com.tcc.pjb.backend.service.processual.recursal.workspace;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationPlaybookResponse;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationResponse;
import com.tcc.pjb.backend.service.processual.recursal.automation.RecursalAutomationPlaybookService;
import com.tcc.pjb.backend.service.processual.recursal.automation.RecursalAutomationService;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RecursalGuidedPieceTrackFactoryTest {

    private final RecursalAutomationService automationService = new RecursalAutomationService();
    private final RecursalAutomationPlaybookService playbookService = new RecursalAutomationPlaybookService(automationService);

    @Test
    void deveGerarTrilhaGuiadaExcepcionalESuplementarQuandoHouverDivergenciaInterna() {
        RecursalAutomationRequest request = new RecursalAutomationRequest(
                "ACORDAO",
                "REFORMAR",
                false,
                false,
                false,
                false,
                true,
                true,
                false,
                false,
                false,
                false,
                false,
                true,
                true,
                false,
                false,
                false,
                false,
                Set.of()
        );

        RecursalAutomationResponse response = automationService.advise(request);
        RecursalAutomationPlaybookResponse playbook = playbookService.buildPlaybook(request);

        assertThat(RecursalGuidedPieceTrackFactory.buildTracks(playbook.rotaPrioritaria(), response, request, playbook))
                .extracting(track -> track.codigo())
                .containsExactly("AGRAVO_RECURSO_EXCEPCIONAL_GUIADO", "EMBARGOS_DIVERGENCIA_GUIADO");
    }
}
