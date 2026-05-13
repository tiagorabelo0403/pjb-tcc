package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProceduralIntelligenceAdvisorTest {

    @Test
    void deveInferirNaturezaMandamentalConstitucionalComUrgencia() {
        ProceduralIntelligenceAdvisoryReport report = ProceduralIntelligenceAdvisor.analyzeRouting(
                Map.of(
                        "classeProcessual", "Mandado de Segurança",
                        "assunto", "fornecimento de medicamento de alto custo",
                        "pedidoPrincipal", "liminar contra ato coator para garantir medicamento e leito",
                        "tipoJustica", "FEDERAL"
                ),
                "MANDADO_SEGURANCA",
                "CONSTITUCIONAL",
                TipoJustica.FEDERAL,
                "MANDADO_SEGURANCA",
                "120",
                "Mandado de Segurança",
                "ALTA",
                "PRE_CONSTITUIDA",
                0.91d,
                "MEDIO"
        );

        assertEquals(NaturezaJuridicaCanonical.MANDAMENTAL, report.naturezaPrincipal());
        assertEquals(TipoJustica.FEDERAL, report.suggestedTipoJustica());
        assertEquals(RamoDireito.CONSTITUCIONAL, report.suggestedRamo());
        assertEquals(MateriaJurisdicao.SAUDE, report.suggestedMateria());
        assertTrue(report.qualifiers().contains(NaturezaJuridicaQualifier.URGENTE));
        assertEquals(NivelSigilo.PUBLICO, report.suggestedSigilo() == null ? NivelSigilo.PUBLICO : report.suggestedSigilo());
    }

    @Test
    void deveInferirNaturezaExecutivaFiscalComDocumentosEssenciais() {
        ProceduralIntelligenceAdvisoryReport report = ProceduralIntelligenceAdvisor.analyzeRouting(
                Map.of(
                        "classeProcessual", "Execução Fiscal",
                        "assunto", "cobrança de dívida ativa tributária",
                        "pedidoPrincipal", "executar CDA referente a IPTU inadimplido"
                ),
                "EXECUCAO_FISCAL",
                "TRIBUTARIO",
                TipoJustica.ESTADUAL,
                RitoProcessual.EXECUCAO_FISCAL.name(),
                "1116",
                "Execução Fiscal",
                "MEDIA",
                "DOCUMENTAL",
                0.93d,
                "MEDIO"
        );

        assertEquals(NaturezaJuridicaCanonical.EXECUTIVA, report.naturezaPrincipal());
        assertEquals(RamoDireito.TRIBUTARIO, report.suggestedRamo());
        assertEquals(RitoProcessual.EXECUCAO_FISCAL, report.suggestedRito());
        assertEquals(MateriaJurisdicao.EXECUCAO_FISCAL, report.suggestedMateria());
        assertTrue(report.recommendedDocuments().contains("certidao_de_divida_ativa"));
        assertTrue(report.recommendedDocuments().contains("titulo_executivo"));
    }
}
