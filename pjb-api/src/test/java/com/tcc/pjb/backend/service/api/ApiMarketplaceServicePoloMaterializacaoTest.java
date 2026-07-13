package com.tcc.pjb.backend.service.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.model.entity.enums.TipoParte;
import com.tcc.pjb.backend.model.entity.enums.TipoPolo;
import com.tcc.pjb.backend.model.entity.processo.PoloProcessual;
import com.tcc.pjb.backend.model.repository.PoloProcessualRepository;
import com.tcc.pjb.backend.service.triagem.TriagemNacionalIAEngine;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Tag("integration")
@TestPropertySource(properties = {
        "spring.cache.type=none",
        "pjb.workflow.enabled=false",
        "pjb.outbox.ingress.enabled=false"
})
class ApiMarketplaceServicePoloMaterializacaoTest extends PjbIntegrationTestBase {

    @Autowired
    private ApiMarketplaceService service;

    @Autowired
    private PoloProcessualRepository poloProcessualRepository;

    @MockitoBean
    private MarketplaceGovernanceService governanceService;

    @MockitoBean
    private TriagemNacionalIAEngine triagemNacionalIAEngine;

    @Test
    void protocolarViaMarketplaceMaterializaPolosAutorEReu() {
        doNothing().when(governanceService).assertClientCanProtocol(anyString());
        doNothing().when(governanceService).registrarConsumoProtocolo(anyString());
        when(governanceService.publicarEventoProtocolo(anyString(), org.mockito.ArgumentMatchers.any(), anyString(), anyString()))
                .thenReturn(List.of());

        ApiMarketplaceService.MarketplaceProtocoloRequest request = new ApiMarketplaceService.MarketplaceProtocoloRequest(
                "ref-001",
                "0009999-11.2026.8.06.0001",
                "ESTADUAL",
                "CIVIL",
                "CE",
                "Fortaleza",
                "ACAO_DE_COBRANCA",
                "Cobranca via marketplace",
                "Condenacao ao pagamento",
                null,
                "Cliente Marketplace Ltda",
                "12345678000199",
                "Fornecedor Reu Ltda",
                "98765432000188",
                BigDecimal.valueOf(5000)
        );

        ApiMarketplaceService.MarketplaceProtocoloResponse result = service.protocolar(request, "client-teste");

        assertThat(result.processoId()).isNotNull();
        List<PoloProcessual> polos = poloProcessualRepository.findByProcessoIdAndAtivo(result.processoId(), true);
        assertThat(polos).hasSize(2);
        assertThat(polos).anySatisfy(polo -> {
            assertThat(polo.getTipoPolo()).isEqualTo(TipoPolo.ATIVO);
            assertThat(polo.getTipoParte()).isEqualTo(TipoParte.AUTOR);
            assertThat(polo.getNomeCompleto()).isEqualTo("Cliente Marketplace Ltda");
        });
        assertThat(polos).anySatisfy(polo -> {
            assertThat(polo.getTipoPolo()).isEqualTo(TipoPolo.PASSIVO);
            assertThat(polo.getTipoParte()).isEqualTo(TipoParte.REU);
            assertThat(polo.getNomeCompleto()).isEqualTo("Fornecedor Reu Ltda");
        });
    }
}
