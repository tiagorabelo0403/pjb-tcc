package com.tcc.pjb.backend.service.processual.calculo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CalculoJudicialDomainSupportTest {

    @Test
    void deveNormalizarAliasesDeDominio() {
        assertEquals("TRABALHISTA_CLT", CalculoJudicialDomainSupport.normalize("trabalhista-clt"));
        assertEquals("TRABALHISTA_CLT", CalculoJudicialDomainSupport.normalize("Trabalhista"));
        assertEquals("FAZENDA_TRIBUTARIO", CalculoJudicialDomainSupport.normalize("fazenda e tributario"));
        assertEquals("CUSTAS_PROCESSUAIS", CalculoJudicialDomainSupport.normalize("custas e despesas"));
        assertEquals("FEDERAL_PREVIDENCIARIO_CJF", CalculoJudicialDomainSupport.normalize("jef-previdenciario"));
    }

    @Test
    void deveGerarSlugEPrefixoCanonicos() {
        assertEquals("trabalhista-clt", CalculoJudicialDomainSupport.slug("TRABALHISTA_CLT"));
        assertEquals("pjb-calculo-fazenda-tributario", CalculoJudicialDomainSupport.filenamePrefix("FAZENDA_TRIBUTARIO"));
        assertEquals("Fazenda e Tributário", CalculoJudicialDomainSupport.aba("FAZENDA_TRIBUTARIO"));
        assertEquals("custas-processuais", CalculoJudicialDomainSupport.slug("CUSTAS_PROCESSUAIS"));
        assertEquals("federal-previdenciario-cjf", CalculoJudicialDomainSupport.slug("FEDERAL_PREVIDENCIARIO_CJF"));
    }

    @Test
    void deveReconhecerFiltroPorCodigoSlugOuAba() {
        assertTrue(CalculoJudicialDomainSupport.matches("trabalhista-clt", "TRABALHISTA_CLT", "Trabalhista CLT"));
        assertTrue(CalculoJudicialDomainSupport.matches("fazenda tributario", "FAZENDA_TRIBUTARIO", "Fazenda e Tributário"));
        assertTrue(CalculoJudicialDomainSupport.matches("custas-processuais", "CUSTAS_PROCESSUAIS", "Custas e Despesas"));
        assertTrue(CalculoJudicialDomainSupport.matches("federal-previdenciario-cjf", "FEDERAL_PREVIDENCIARIO_CJF", "Federal/JEF Previdenciário"));
    }

    @Test
    void deveExporRotasEAliasesCanonicos() {
        assertTrue(CalculoJudicialDomainSupport.isSupported("tributário"));
        assertTrue(CalculoJudicialDomainSupport.isSupported("custas"));
        assertTrue(CalculoJudicialDomainSupport.isSupported("cjf-previdenciario"));
        assertTrue(CalculoJudicialDomainSupport.aliases("FAZENDA_TRIBUTARIO").contains("tributario"));
        assertEquals("/api/v1/processual/calculos", CalculoJudicialDomainSupport.basePath());
        assertEquals("/api/v1/processual/calculos/workspace/trabalhista-clt/ajuda", CalculoJudicialDomainSupport.apiRoutes("TRABALHISTA_CLT").get("ajuda"));
        assertEquals("/api/v1/processual/calculos/catalogo/trabalhista-clt/bootstrap", CalculoJudicialDomainSupport.bootstrapRoute("TRABALHISTA_CLT"));
        assertEquals("/api/v1/processual/calculos/tabelas/oficiais/trabalhista-clt", CalculoJudicialDomainSupport.officialTablesRoute("TRABALHISTA_CLT"));
        assertEquals("/api/v1/processual/trabalhista/verbas-rescisorias", CalculoJudicialDomainSupport.apiRoutes("TRABALHISTA_CLT").get("legacyVerbasRescisorias"));
        assertEquals("/api/v1/processual/calculos/tabelas/oficiais/trabalhista-clt", CalculoJudicialDomainSupport.apiRoutes("TRABALHISTA_CLT").get("tabelasOficiais"));
        assertEquals("/api/v1/processual/calculos/ia/financeira/trabalhista-clt", CalculoJudicialDomainSupport.apiRoutes("TRABALHISTA_CLT").get("iaFinanceira"));
        assertEquals("/api/v1/processual/calculos/referencias/economicas", CalculoJudicialDomainSupport.apiRoutes("TRABALHISTA_CLT").get("referenciasEconomicas"));
        assertEquals("/api/v1/processual/calculos/experiencia/preferencia", CalculoJudicialDomainSupport.apiRoutes("TRABALHISTA_CLT").get("experiencePreference"));
        assertEquals("/api/v1/processual/calculos/ia/financeira/sinalizar-ajuizamento", CalculoJudicialDomainSupport.apiRoutes("TRABALHISTA_CLT").get("liveAjuizamentoAssist"));
        assertEquals("canonical_first", CalculoJudicialDomainSupport.routePolicy("TRABALHISTA_CLT").get("compatibilityMode"));
    }

    @Test
    void deveResolverDominioPeloPathQuandoPossivel() {
        assertEquals("TRABALHISTA_CLT", CalculoJudicialDomainSupport.fromPath("/api/v1/processual/calculos/trabalhista-clt/pdf"));
        assertEquals("FAZENDA_TRIBUTARIO", CalculoJudicialDomainSupport.fromPath("/api/v1/processual/calculos/assistente/fazenda-tributario"));
        assertEquals("CUSTAS_PROCESSUAIS", CalculoJudicialDomainSupport.fromPath("/api/v1/processual/calculos/custas-processuais/pdf"));
        assertEquals("FEDERAL_PREVIDENCIARIO_CJF", CalculoJudicialDomainSupport.fromPath("/api/v1/processual/calculos/federal-previdenciario-cjf/pdf"));
    }

    @Test
    void deveRejeitarDominioNaoSuportado() {
        assertThrows(CalculoJudicialUnsupportedDomainException.class, () -> CalculoJudicialDomainSupport.requireSupported("penal"));
    }
}
