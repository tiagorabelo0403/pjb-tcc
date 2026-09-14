package com.tcc.pjb.backend.ai.juridica.conversation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.ai.juridica.spine.JuridicaHallucinationGuardService;
import com.tcc.pjb.backend.ai.juridica.spine.JuridicaResearchDossierService;
import com.tcc.pjb.backend.ai.juridica.spine.JuridicaValidationEnvelopeService;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalHallucinationGuardRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalHallucinationGuardResponse;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalResearchDossierRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalResearchDossierResponse;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalValidationRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalValidationResponse;
import org.junit.jupiter.api.Test;

class LegalAiJuridicaSpineOrchestratorTest {

    private final JuridicaResearchDossierService dossierService = mock(JuridicaResearchDossierService.class);
    private final JuridicaValidationEnvelopeService validationService = mock(JuridicaValidationEnvelopeService.class);
    private final JuridicaHallucinationGuardService guardService = mock(JuridicaHallucinationGuardService.class);
    private final LegalAiJuridicaSpineOrchestrator orchestrator = new LegalAiJuridicaSpineOrchestrator(dossierService, validationService, guardService);

    @Test
    void buildDossierDelega() {
        var request = mock(LegalResearchDossierRequest.class);
        var response = mock(LegalResearchDossierResponse.class);
        when(dossierService.build(request)).thenReturn(response);

        assertThat(orchestrator.buildDossier(request)).isSameAs(response);
    }

    @Test
    void validateDelega() {
        var request = mock(LegalValidationRequest.class);
        var response = mock(LegalValidationResponse.class);
        when(validationService.validate(request)).thenReturn(response);

        assertThat(orchestrator.validate(request)).isSameAs(response);
    }

    @Test
    void evaluateGuardDelega() {
        var request = mock(LegalHallucinationGuardRequest.class);
        var response = mock(LegalHallucinationGuardResponse.class);
        when(guardService.evaluate(request)).thenReturn(response);

        assertThat(orchestrator.evaluateGuard(request)).isSameAs(response);
    }
}
