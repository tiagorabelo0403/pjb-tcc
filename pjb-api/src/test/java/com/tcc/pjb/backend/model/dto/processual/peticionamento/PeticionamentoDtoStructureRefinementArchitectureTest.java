package com.tcc.pjb.backend.model.dto.processual.peticionamento;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.dto.processual.peticionamento.governance.PeticionamentoAutomacaoResponse;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.governance.PeticionamentoGuardrailResponse;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.journey.PeticionamentoJourneyIntelligenceResponse;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.journey.PeticionamentoSimpleProtocolWizardResponse;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.media.PeticionamentoMediaBlocoRequest;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.session.PeticionamentoSessaoRequest;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.session.PeticionamentoSessaoResponse;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.studio.PeticionamentoStudioWorkspaceResponse;
import org.junit.jupiter.api.Test;

class PeticionamentoDtoStructureRefinementArchitectureTest {

    @Test
    void dtosDevemViverEmSubpacotesDedicados() {
        assertThat(PeticionamentoAutomacaoResponse.class.getPackageName()).endsWith(".governance");
        assertThat(PeticionamentoGuardrailResponse.class.getPackageName()).endsWith(".governance");
        assertThat(PeticionamentoJourneyIntelligenceResponse.class.getPackageName()).endsWith(".journey");
        assertThat(PeticionamentoSimpleProtocolWizardResponse.class.getPackageName()).endsWith(".journey");
        assertThat(PeticionamentoMediaBlocoRequest.class.getPackageName()).endsWith(".media");
        assertThat(PeticionamentoSessaoRequest.class.getPackageName()).endsWith(".session");
        assertThat(PeticionamentoSessaoResponse.class.getPackageName()).endsWith(".session");
        assertThat(PeticionamentoStudioWorkspaceResponse.class.getPackageName()).endsWith(".studio");
    }
}
