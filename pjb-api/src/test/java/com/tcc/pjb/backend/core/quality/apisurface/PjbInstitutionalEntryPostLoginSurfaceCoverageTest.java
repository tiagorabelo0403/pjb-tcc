package com.tcc.pjb.backend.core.quality.apisurface;

import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry.NationalCommunicationInstitutionalEntryActivationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry.NationalCommunicationInstitutionalEntrySummaryResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalOperationalProfileResponse;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.surface.NationalCommunicationInstitutionalSurfaceFacadeService;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PjbInstitutionalEntryPostLoginSurfaceCoverageTest {

    @Test
    void intelligentEntryMustExposeOperationalProfileAndPostLoginActivation() {
        String dto = ApiSurfaceTestSupport.read(Path.of("src/main/java/com/tcc/pjb/backend/model/dto/processual/comunicacao/institutional/entry/NationalCommunicationInstitutionalEntrySummaryResponse.java"));
        String facade = ApiSurfaceTestSupport.read(Path.of("src/main/java/com/tcc/pjb/backend/service/processual/comunicacao/institutional/surface/NationalCommunicationInstitutionalSurfaceFacadeService.java"));
        assertTrue(dto.contains("NationalCommunicationInstitutionalOperationalProfileResponse perfilOperacionalAtivo"));
        assertTrue(dto.contains("NationalCommunicationInstitutionalEntryActivationResponse ativacaoPosLogin"));
        assertTrue(facade.contains("entryActivationDecisionApplicationService.avaliarEntradaAtual(summary)"));
        assertTrue(facade.contains("toOperationalProfile(operationalProfile)"));
        assertTrue(facade.contains("toActivation(activationDecision)"));

        String activation = ApiSurfaceTestSupport.read(Path.of("src/main/java/com/tcc/pjb/backend/model/dto/processual/comunicacao/institutional/entry/NationalCommunicationInstitutionalEntryActivationResponse.java"));
        assertTrue(activation.contains("boolean panelProvisioningComplete"));
        assertTrue(activation.contains("boolean sharedExperienceReady"));
        assertTrue(activation.contains("boolean requiresPanelProvisioningReview"));
    }
}