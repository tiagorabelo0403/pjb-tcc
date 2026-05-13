package com.tcc.pjb.backend.service.processual.calculo;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;

class CalculoJudicialApiObservabilityServiceTest {

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final CalculoJudicialFrontendContractService contractService = new CalculoJudicialFrontendContractService(new CalculoJudicialTabelaOficialService(), TestEconomicReferenceSupport.economicReferenceService());
    private final CalculoJudicialApiObservabilityService service = new CalculoJudicialApiObservabilityService(contractService, meterRegistry);

    @Test
    void deveResolverContextoCanonicoEAplicarHeadersObservaveis() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/processual/calculos/catalogo/trabalhista-clt/bootstrap");
        CalculoJudicialApiRouteContext context = service.fromRequest(request);
        HttpHeaders headers = new HttpHeaders();

        service.apply(headers, context);
        service.record(context, "GET", 200);

        assertThat(context.apiFamily()).isEqualTo("calculos");
        assertThat(context.operation()).isEqualTo("bootstrap");
        assertThat(context.domain()).isEqualTo("TRABALHISTA_CLT");
        assertThat(headers.getFirst("X-PJB-Api-Route-Status")).isEqualTo("canonical");
        assertThat(headers.getFirst("X-PJB-Api-Operation")).isEqualTo("bootstrap");
        assertThat(headers.getFirst("X-PJB-Calculation-Domain")).isEqualTo("TRABALHISTA_CLT");
        assertThat(headers.getFirst(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS)).contains("X-Request-Id");
        assertThat(meterRegistry.get("pjb.calculo.api.requests").counter().count()).isEqualTo(1.0d);
    }


    @Test
    void deveResolverContextoDeTabelasOficiais() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/processual/calculos/tabelas/oficiais/federal-previdenciario-cjf");
        CalculoJudicialApiRouteContext context = service.fromRequest(request);

        assertThat(context.apiFamily()).isEqualTo("calculos");
        assertThat(context.operation()).isEqualTo("tabelas_oficiais_dominio");
        assertThat(context.domain()).isEqualTo("FEDERAL_PREVIDENCIARIO_CJF");
    }


    @Test
    void deveResolverContextoDaIaFinanceira() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/processual/calculos/ia/financeira/custas-processuais");
        CalculoJudicialApiRouteContext context = service.fromRequest(request);

        assertThat(context.apiFamily()).isEqualTo("calculos");
        assertThat(context.operation()).isEqualTo("ia_financeira");
        assertThat(context.domain()).isEqualTo("CUSTAS_PROCESSUAIS");
    }

    @Test
    void deveResolverContextoDaPreferenciaDeExperiencia() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/processual/calculos/experiencia/preferencia");
        CalculoJudicialApiRouteContext context = service.fromRequest(request);

        assertThat(context.apiFamily()).isEqualTo("calculos");
        assertThat(context.operation()).isEqualTo("experience_preference");
    }

    @Test
    void deveResolverContextoLegacyEContarErrosSeparadamente() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/processual/trabalhista/verbas-rescisorias");
        CalculoJudicialApiRouteContext context = service.fromRequest(request);
        HttpHeaders headers = new HttpHeaders();

        service.apply(headers, context);
        service.record(context, "POST", 422);

        assertThat(context.apiFamily()).isEqualTo("trabalhista_legacy");
        assertThat(context.routeStatus()).isEqualTo("compatibility");
        assertThat(headers.getFirst("Deprecation")).isEqualTo("true");
        assertThat(headers.getFirst("X-PJB-Preferred-Route")).isEqualTo("/api/v1/processual/calculos/trabalhista-clt");
        assertThat(meterRegistry.get("pjb.calculo.api.errors").tag("family", "trabalhista_legacy").counter().count()).isEqualTo(1.0d);
    }
}
