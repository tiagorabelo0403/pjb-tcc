package com.tcc.pjb.backend.platform.jusos.v2.jurimetria;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.ai.jurimetria.model.JurimetriaReport;
import com.tcc.pjb.backend.model.entity.enums.TipoPrecedente;
import com.tcc.pjb.backend.model.entity.enums.TribunalFonte;
import com.tcc.pjb.backend.model.entity.jurisprudencia.Precedente;
import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine;
import com.tcc.pjb.backend.platform.jusos.v2.rules.NationalRulePackEngine;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class JurimetriaNarrativeSupportTest {

    private final JurimetriaNarrativeSupport support = new JurimetriaNarrativeSupport();

    @Test
    void deveFormatarPrecedentesEConstruirMetodologia() {
        List<Precedente> precedentes = List.of(
                Precedente.builder().fonte(TribunalFonte.STJ).tipo(TipoPrecedente.TEMA_REPETITIVO).identificador("Tema 123").titulo("Tema repetitivo sobre devolução em dobro").dataPublicacao(LocalDate.now()).build(),
                Precedente.builder().fonte(TribunalFonte.TJ).tipo(TipoPrecedente.ACORDAO).titulo("Acórdão local de apoio").dataPublicacao(LocalDate.now().minusDays(1)).build()
        );
        NationalRulePackEngine.ResultadoRegras regras = new NationalRulePackEngine.ResultadoRegras(List.of(), List.of(), List.of("contrato"), false, 1);
        NationalPrazoEngine.PrazoCalculado prazo = new NationalPrazoEngine.PrazoCalculado(
                LocalDate.now(),
                LocalDate.now().plusDays(10),
                10,
                8,
                NationalPrazoEngine.TipoPrazo.APELACAO,
                null,
                null,
                false,
                List.of(),
                "CPC"
        );
        JurimetriaReport relatorioIA = JurimetriaReport.builder().explicacao("Modelo de IA encontrou alinhamento com precedentes qualificados e baixa dispersão argumentativa.").build();

        List<String> formatados = support.formatarPrecedentes(precedentes);
        String metodologia = support.construirMetodologia(regras, prazo, relatorioIA, precedentes.size());

        assertThat(formatados.getFirst()).contains("Tema 123").contains("STJ");
        assertThat(metodologia)
                .contains("NationalPrazoEngine")
                .contains("Pesquisa de precedentes indexados: 2")
                .contains("Modelo de IA encontrou alinhamento");
    }
}
