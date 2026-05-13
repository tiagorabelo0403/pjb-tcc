package com.tcc.pjb.backend.service.processual.calculo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CalculoJudicialTabelaOficialServiceTest {

    private final CalculoJudicialTabelaOficialService service = new CalculoJudicialTabelaOficialService();

    @Test
    void deveExporCatalogoGlobalEPerfilPorDominio() {
        var catalogo = service.catalog(null);
        var perfil = service.profile("FEDERAL_PREVIDENCIARIO_CJF");

        assertThat(catalogo.tabelas()).hasSizeGreaterThanOrEqualTo(4);
        assertThat(catalogo.rotas()).containsEntry("catalogo", CalculoJudicialDomainSupport.officialTablesRoute());
        assertThat(perfil).containsEntry("route", CalculoJudicialDomainSupport.officialTablesRoute("FEDERAL_PREVIDENCIARIO_CJF"));
        assertThat(perfil.toString()).contains("CJF");
    }
}
