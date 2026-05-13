package com.tcc.pjb.backend.service.processual.calculo;

import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialAssistenciaResponse;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialSolicitantePerfil;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CalculoJudicialAssistenciaServiceTest {

    private final CalculoJudicialAssistenciaService service = new CalculoJudicialAssistenciaService(new CalculoJudicialProfileResolverService(), new CalculoJudicialFrontendContractService(new CalculoJudicialTabelaOficialService(), TestEconomicReferenceSupport.economicReferenceService()));

    @Test
    void deveExporMetadadosDeContratoApiNoFluxoTrabalhista() {
        CalculoJudicialAssistenciaResponse response = service.orientarTrabalhista(null, null);
        var metadata = service.metadataTrabalhista(null, CalculoJudicialSolicitantePerfil.CIDADAO);
        assertEquals("TRABALHISTA_CLT", response.dominio());
        assertTrue(metadata.containsKey("apiContract"));
        assertTrue(metadata.containsKey("profileCapabilities"));
        assertEquals("/api/v1/processual/calculos/workspace/trabalhista-clt/ajuda", ((java.util.Map<?, ?>) metadata.get("apiContract")).get("ajudaRoute"));
    }

    @Test
    void deveExporAliasesCanonicosNoFluxoFazendario() {
        var metadata = service.metadataFazenda(null, CalculoJudicialSolicitantePerfil.PROCURADORIA);
        assertTrue(((java.util.List<?>) metadata.get("apiAliases")).contains("tributario"));
    }

    @Test
    void deveExporAssistenciaDeCustasComRotasCanonicas() {
        var metadata = service.metadataCustas(null, CalculoJudicialSolicitantePerfil.ADVOGADO);
        assertTrue(((java.util.List<?>) metadata.get("apiAliases")).contains("custas"));
        assertEquals("/api/v1/processual/calculos/custas-processuais", ((java.util.Map<?, ?>) metadata.get("apiRoutes")).get("json"));
    }
}
