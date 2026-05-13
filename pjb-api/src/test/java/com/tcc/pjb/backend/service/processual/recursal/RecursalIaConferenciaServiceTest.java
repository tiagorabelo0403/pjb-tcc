package com.tcc.pjb.backend.service.processual.recursal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.tcc.pjb.backend.service.processual.calculo.CalculoJudicialFrontendContractService;
import com.tcc.pjb.backend.service.processual.recursal.ia.RecursalIaPlannerService;
import com.tcc.pjb.backend.service.processual.calculo.CalculoJudicialTabelaOficialService;
import com.tcc.pjb.backend.service.processual.calculo.TestEconomicReferenceSupport;
import com.tcc.pjb.backend.service.processual.surface.ProcessualOperationalSurfaceFacadeService;
import com.tcc.pjb.backend.service.processual.recursal.ia.RecursalIaConferenciaService;
import org.junit.jupiter.api.Test;

class RecursalIaConferenciaServiceTest {

    private final ProcessualOperationalSurfaceFacadeService facadeService = mock(ProcessualOperationalSurfaceFacadeService.class);
    private final CalculoJudicialFrontendContractService contractService = new CalculoJudicialFrontendContractService(new CalculoJudicialTabelaOficialService(), TestEconomicReferenceSupport.economicReferenceService());
    private final RecursalIaPlannerService plannerService = mock(RecursalIaPlannerService.class);
    private final RecursalIaConferenciaService service = new RecursalIaConferenciaService(facadeService, contractService, plannerService);

    @Test
    void deveFicarPendenteQuandoNaoReceberBlocoDeAdmissibilidade() {
        var response = service.conferir(null);

        assertThat(response.conferenciaExecutada()).isFalse();
        assertThat(response.status()).isEqualTo("PENDING_INPUT");
        assertThat(response.pendencias()).isNotEmpty();
        assertThat(response.metadata()).containsEntry("entryRoute", "/api/v1/processual/recursal/ia/conferencia");
    }
}
