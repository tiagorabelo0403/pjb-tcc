package com.tcc.pjb.backend.service.processual.calculo;

import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialSolicitantePerfil;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialWorkspaceCardResponse;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialWorkspaceResponse;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CalculoJudicialWorkspaceServiceTest {

    private final CalculoJudicialFrontendContractService contractService = new CalculoJudicialFrontendContractService(new CalculoJudicialTabelaOficialService(), TestEconomicReferenceSupport.economicReferenceService());
    private final CalculoJudicialExperiencePreferenceService preferenceService = new CalculoJudicialExperiencePreferenceService(org.mockito.Mockito.mock(com.tcc.pjb.backend.repository.ui.UsuarioCalculoExperiencePreferenceRepository.class), contractService);
    private final CalculoJudicialWorkspaceService service = new CalculoJudicialWorkspaceService(new CalculoJudicialProfileResolverService(), contractService, preferenceService);

    @Test
    void deveExporCatalogoApiERotaDeAjudaNoWorkspace() {
        CalculoJudicialWorkspaceResponse response = service.workspace(null, CalculoJudicialSolicitantePerfil.ADVOGADO, "trabalhista-clt");
        assertEquals("Trabalhista CLT", response.abaPadrao());
        assertEquals(1, response.calculadoras().size());
        assertTrue(response.designNavegacao().containsKey("apiCatalog"));
        assertTrue(response.designNavegacao().containsKey("contractFingerprint"));
        assertEquals("/api/v1/processual/calculos/workspace/trabalhista-clt/ajuda", response.calculadoras().get(0).rotas().get("ajuda"));
        assertTrue(response.designNavegacao().containsKey("aiAgents"));
        assertTrue(response.designNavegacao().containsKey("financialIaMessages"));
        assertTrue(response.designNavegacao().containsKey("resolvedExperiencePreference"));
        assertEquals("manual_tradicional", response.designNavegacao().get("defaultExperienceMode"));
    }

    @Test
    void deveRetornarCardDeAjudaPorDominioCanonico() {
        CalculoJudicialWorkspaceCardResponse card = service.workspaceCard(null, CalculoJudicialSolicitantePerfil.CIDADAO, "FAZENDA_TRIBUTARIO");
        assertEquals("FAZENDA_TRIBUTARIO", card.codigo());
        assertEquals("/api/v1/processual/calculos/assistente/fazenda-tributario", card.rotas().get("assistente"));
        assertEquals("/api/v1/processual/calculos/ia/financeira/executar", card.design().get("financialIaRoute"));
        assertEquals("/api/v1/processual/calculos/experiencia/preferencia", card.design().get("experiencePreferenceRoute"));
        assertEquals("assistido_com_ia", ((java.util.Map<?, ?>) card.design().get("iaEntry")).get("code"));
        assertEquals("manual_tradicional", ((java.util.Map<?, ?>) card.design().get("manualEntry")).get("code"));
    }

    @Test
    void deveRejeitarCardParaDominioInvalido() {
        assertThrows(IllegalArgumentException.class, () -> service.workspaceCard(null, CalculoJudicialSolicitantePerfil.CIDADAO, "penal"));
    }
}
