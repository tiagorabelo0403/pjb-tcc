package com.tcc.pjb.backend.platform.jusos.v2.jurimetria;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.ai.jurimetria.model.JurimetriaReport;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.TipoPrecedente;
import com.tcc.pjb.backend.model.entity.enums.TribunalFonte;
import com.tcc.pjb.backend.model.entity.jurisprudencia.Precedente;
import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine;
import com.tcc.pjb.backend.platform.jusos.v2.rules.NationalRulePackEngine;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class JurimetriaAggregationSupportTest {

    private final JurimetriaAggregationSupport support = new JurimetriaAggregationSupport();

    @Test
    void deveCalcularIndicadoresComSinaisLocaisEDeIa() {
        Processo processo = Processo.builder()
                .ramoDireito(RamoDireito.CONSUMIDOR)
                .valorCausa(BigDecimal.valueOf(12500))
                .scoreComplexidade(88)
                .build();
        JurimetriaEngine.BaseLocalAnalitica baseLocal = new JurimetriaEngine.BaseLocalAnalitica(90, 42, 90, 30, 22, 18, 11, 410.0, 0.52, 0.32, 1.33, 0.47, 0.12);
        JurimetriaReport relatorioIA = JurimetriaReport.builder()
                .indicadores(java.util.List.of(JurimetriaReport.Indicador.builder().nome("Taxa Sucesso Estimada").valor(0.77).unidade("ratio").build()))
                .build();
        List<Precedente> precedentes = List.of(
                Precedente.builder().fonte(TribunalFonte.TJ).tipo(TipoPrecedente.ACORDAO).titulo("precedente inválido").build()
        );
        NationalRulePackEngine.ResultadoRegras regras = new NationalRulePackEngine.ResultadoRegras(List.of(), List.of("alerta critico de rito"), List.of(), false, 1);
        NationalPrazoEngine.PrazoCalculado prazo = new NationalPrazoEngine.PrazoCalculado(
                LocalDate.now(),
                LocalDate.now().plusDays(4),
                4,
                4,
                NationalPrazoEngine.TipoPrazo.APELACAO,
                RamoDireito.CONSUMIDOR,
                GrauJurisdicao.SEGUNDO_GRAU,
                false,
                List.of("janela curta"),
                "CPC"
        );

        List<JurimetriaEngine.IndicadorJurimetrico> indicadores = support.calcularIndicadores(
                processo,
                RamoDireito.CONSUMIDOR,
                GrauJurisdicao.SEGUNDO_GRAU,
                "TJCE",
                baseLocal,
                relatorioIA,
                precedentes,
                regras,
                prazo
        );

        assertThat(indicadores)
                .extracting(JurimetriaEngine.IndicadorJurimetrico::nome)
                .contains("total_processos_mesmo_ramo", "janela_recursal_dias_uteis", "complexidade_processual", "ia_taxa_sucesso_estimada");
        assertThat(indicadores)
                .filteredOn(i -> i.nome().equals("tempo_medio_local_dias"))
                .singleElement()
                .extracting(JurimetriaEngine.IndicadorJurimetrico::interpretacao)
                .isEqualTo("Tramitação em faixa moderada");
    }

    @Test
    void deveConstruirPerfilDoTribunalComOrgaoEFrequenciaDeTeses() {
        JurimetriaEngine.BaseLocalAnalitica baseLocal = new JurimetriaEngine.BaseLocalAnalitica(50, 21, 50, 17, 8, 5, 2, 330.0, 0.38, 0.19, 1.12, 0.51, 0.09);
        List<Precedente> precedentes = List.of(
                Precedente.builder().fonte(TribunalFonte.TJ).tipo(TipoPrecedente.ACORDAO).tese("Tema do consumidor 1").dataPublicacao(LocalDate.now()).build(),
                Precedente.builder().fonte(TribunalFonte.TJ).tipo(TipoPrecedente.ACORDAO).tese("Tema do consumidor 2").dataPublicacao(LocalDate.now().minusDays(1)).build()
        );

        JurimetriaEngine.PerfisDecisional perfil = support.construirPerfilTribunal("TJCE", TribunalFonte.TJ, RamoDireito.CONSUMIDOR, baseLocal, precedentes);

        assertThat(perfil.orgaoJulgador()).contains("Tribunal de Justiça");
        assertThat(perfil.tesesMaisFrequentes()).contains("Tema do consumidor 1", "Tema do consumidor 2");
        assertThat(perfil.taxaProvimento()).isBetween(0.0, 1.0);
        assertThat(perfil.taxaDesprovimento()).isBetween(0.0, 1.0);
    }
}
