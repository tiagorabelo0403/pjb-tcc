package com.tcc.pjb.backend.service.processual.calculo;

import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialFrontendBootstrapResponse;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialExperienceContext;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialFrontendCatalogResponse;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialSolicitantePerfil;
import com.tcc.pjb.backend.service.financeiro.SalarioMinimoNacionalService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CalculoJudicialFrontendCatalogServiceTest {

    private static final BigDecimal SALARIO_MOCK = new BigDecimal("1621.00");

    private final CalculoJudicialFrontendContractService contractService = new CalculoJudicialFrontendContractService(new CalculoJudicialTabelaOficialService(), TestEconomicReferenceSupport.economicReferenceService());
    private final CalculoJudicialExperiencePreferenceService preferenceService = new CalculoJudicialExperiencePreferenceService(org.mockito.Mockito.mock(com.tcc.pjb.backend.repository.ui.UsuarioCalculoExperiencePreferenceRepository.class), contractService);
    private final SalarioMinimoNacionalService salarioMinimoNacionalService = salarioServiceComValorMockado(SALARIO_MOCK);
    private final CalculoJudicialFrontendCatalogService service = new CalculoJudicialFrontendCatalogService(new CalculoJudicialProfileResolverService(), contractService, new CalculoJudicialTabelaOficialService(), preferenceService, salarioMinimoNacionalService);

    private static SalarioMinimoNacionalService salarioServiceComValorMockado(BigDecimal valor) {
        SalarioMinimoNacionalService mocked = mock(SalarioMinimoNacionalService.class);
        when(mocked.valorVigente()).thenReturn(valor);
        return mocked;
    }

    @Test
    void deveRetornarCatalogoCompletoParaFrontend() {
        CalculoJudicialFrontendCatalogResponse response = service.catalog(null, CalculoJudicialSolicitantePerfil.ADVOGADO, null);

        assertThat(response.menuPrincipal()).isEqualTo("Calculadora");
        assertThat(response.dominios()).hasSize(4);
        assertThat(response.erros()).containsEntry("frontendCatalogRoute", CalculoJudicialDomainSupport.catalogRoute());
        assertThat(response.erros()).containsEntry("frontendOfficialTablesRoute", CalculoJudicialDomainSupport.officialTablesRoute());
        assertThat(response.ui()).containsKeys("contractVersion", "contractFingerprint");
        assertThat(response.dominios().get(0).http()).containsKey("success");
    }

    @Test
    void deveFiltrarCatalogoPorDominioCanonico() {
        CalculoJudicialFrontendCatalogResponse response = service.catalog(null, CalculoJudicialSolicitantePerfil.CIDADAO, "tributário");

        assertThat(response.dominios()).singleElement().satisfies(domain -> {
            assertThat(domain.codigo()).isEqualTo("FAZENDA_TRIBUTARIO");
            assertThat(domain.rotas()).containsEntry("json", "/api/v1/processual/calculos/fazenda-tributario");
            assertThat(domain.payloadInicial()).containsKey("principal");
        });
    }

    @Test
    void deveExporBootstrapComPayloadInicialEExemplos() {
        CalculoJudicialFrontendBootstrapResponse response = service.bootstrap(null, CalculoJudicialSolicitantePerfil.ADVOGADO, "trabalhista-clt");

        assertThat(response.codigo()).isEqualTo("TRABALHISTA_CLT");
        assertThat(response.rotas()).containsEntry("bootstrap", CalculoJudicialDomainSupport.bootstrapRoute("TRABALHISTA_CLT"));
        assertThat(response.http()).containsEntry("fingerprint", contractService.fingerprint());
        assertThat(response.http()).containsKey("resolvedExperiencePreference");
        assertThat(response.http()).containsKey("resolvedExperiencePreferencesByDomain");
        assertThat(response.aiAgents()).containsKey("financeira");
        assertThat(response.officialTables()).containsEntry("route", CalculoJudicialDomainSupport.officialTablesRoute("TRABALHISTA_CLT"));
        assertThat(response.payloadInicial()).containsEntry("perfilSolicitante", "ADVOGADO");
        assertThat(response.iaRequestExemplo()).containsEntry("agentCode", "IA_FINANCEIRA_PJB");
        assertThat(response.requestExemplo()).containsEntry("tipoDispensa", "DISPENSA_SEM_JUSTA_CAUSA");
        assertThat(response.responseExemplo()).containsKey("metadata");
        assertThat(response.errorExemplo()).containsEntry("status", 422);
    }

    @Test
    void deveExporBootstrapDeCustasComEstruturaInicial() {
        CalculoJudicialFrontendBootstrapResponse response = service.bootstrap(null, CalculoJudicialSolicitantePerfil.ADVOGADO, "custas-processuais");

        assertThat(response.codigo()).isEqualTo("CUSTAS_PROCESSUAIS");
        assertThat(response.rotas()).containsEntry("json", "/api/v1/processual/calculos/custas-processuais");
        assertThat(response.payloadInicial()).containsEntry("percentualTaxaJudiciaria", "0.015");
        assertThat(response.requestExemplo()).containsEntry("sistemaOrigem", "e-SAJ");
    }

    @Test
    void deveExporBootstrapFederalPrevidenciarioComEstruturaInicial() {
        CalculoJudicialFrontendBootstrapResponse response = service.bootstrap(null, CalculoJudicialSolicitantePerfil.ADVOGADO, "federal-previdenciario-cjf");

        assertThat(response.codigo()).isEqualTo("FEDERAL_PREVIDENCIARIO_CJF");
        assertThat(response.rotas()).containsEntry("json", "/api/v1/processual/calculos/federal-previdenciario-cjf");
        assertThat(response.payloadInicial()).containsEntry("tipoBeneficio", "Auxílio por incapacidade temporária");
        assertThat(response.requestExemplo()).containsEntry("tribunal", "TRF5");
        assertThat(response.responseExemplo()).containsEntry("dominio", "FEDERAL_PREVIDENCIARIO_CJF");
    }

    @Test
    void salarioMinimoReferenciaVemDoServiceCanonicoNaoDeLiteralAntigo() {
        CalculoJudicialFrontendBootstrapResponse response = service.bootstrap(null, CalculoJudicialSolicitantePerfil.ADVOGADO, "federal-previdenciario-cjf");

        assertThat(response.payloadInicial())
                .as("payloadInicial deve refletir SalarioMinimoNacionalService.valorVigente() mockado, nunca o literal antigo 1518.00")
                .containsEntry("salarioMinimoReferencia", SALARIO_MOCK.toPlainString());
        assertThat(response.requestExemplo())
                .as("requestExemplo deve refletir SalarioMinimoNacionalService.valorVigente() mockado")
                .containsEntry("salarioMinimoReferencia", SALARIO_MOCK.toPlainString());
    }

}