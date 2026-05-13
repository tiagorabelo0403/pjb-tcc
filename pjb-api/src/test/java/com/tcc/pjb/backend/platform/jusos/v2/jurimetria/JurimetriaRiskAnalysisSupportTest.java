package com.tcc.pjb.backend.platform.jusos.v2.jurimetria;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.ai.jurimetria.model.JurimetriaReport;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.TipoPrecedente;
import com.tcc.pjb.backend.model.entity.enums.TribunalFonte;
import com.tcc.pjb.backend.model.entity.jurisprudencia.Precedente;
import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine;
import com.tcc.pjb.backend.platform.jusos.v2.rules.NationalRulePackEngine;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class JurimetriaRiskAnalysisSupportTest {

    private final JurimetriaRiskAnalysisSupport support = new JurimetriaRiskAnalysisSupport();

    @Test
    void deveCalcularRiscoEAlertasComCriticidadeRecursalESigilo() {
        Processo processo = Processo.builder()
                .ramoDireito(RamoDireito.TRIBUTARIO)
                .faseAtual(FaseProcessual.RECURSAL)
                .statusProcesso(StatusProcesso.RECURSO_INTERPOSTO)
                .nivelSigilo(NivelSigilo.SIGILO_N2)
                .valorCausa(BigDecimal.valueOf(150000))
                .scoreComplexidade(92)
                .build();
        JurimetriaEngine.BaseLocalAnalitica baseLocal = new JurimetriaEngine.BaseLocalAnalitica(80, 31, 80, 22, 21, 9, 4, 880.0, 0.67, 0.31, 1.77, 0.29, 0.52);
        JurimetriaEngine.PerfisDecisional perfil = new JurimetriaEngine.PerfisDecisional("TJCE", "Câmara", 0.31, 0.52, 0.11, 22, 880.0, 1.77, 0.31, List.of("Tema repetitivo"));
        List<Precedente> precedentes = List.of(Precedente.builder().fonte(TribunalFonte.TJ).tipo(TipoPrecedente.ACORDAO).tese("Tema repetitivo").build());
        NationalRulePackEngine.ResultadoRegras regras = new NationalRulePackEngine.ResultadoRegras(List.of(), List.of("alerta critico de admissibilidade"), List.of("custas"), false, 1);
        JurimetriaReport relatorioIA = JurimetriaReport.builder()
                .indicadores(java.util.List.of(
                        JurimetriaReport.Indicador.builder().nome("Taxa Sucesso Estimada").valor(0.28).unidade("ratio").build(),
                        JurimetriaReport.Indicador.builder().nome("Prob Tutela Urgencia").valor(0.44).unidade("ratio").build()
                ))
                .build();
        NationalPrazoEngine.PrazoCalculado prazo = new NationalPrazoEngine.PrazoCalculado(
                LocalDate.now(),
                LocalDate.now().plusDays(3),
                3,
                3,
                NationalPrazoEngine.TipoPrazo.RECURSO_ESPECIAL,
                RamoDireito.TRIBUTARIO,
                GrauJurisdicao.SUPERIOR,
                false,
                List.of("janela critica"),
                "CPC"
        );

        JurimetriaEngine.RiscoJuridico risco = support.calcularRisco(
                processo,
                RamoDireito.TRIBUTARIO,
                GrauJurisdicao.SUPERIOR,
                perfil,
                precedentes,
                regras,
                relatorioIA,
                prazo,
                baseLocal
        );
        List<String> alertas = support.gerarAlertasEstrategicos(
                processo,
                RamoDireito.TRIBUTARIO,
                GrauJurisdicao.CONSTITUCIONAL,
                risco,
                perfil,
                regras,
                prazo,
                baseLocal,
                precedentes
        );
        List<JurimetriaEngine.CenarioEstrategico> cenarios = support.construirCenarios(processo, risco, baseLocal, perfil, prazo);

        assertThat(risco.probabilidadeExito()).isLessThan(0.40);
        assertThat(risco.nivel()).isIn(JurimetriaEngine.NivelRisco.DESFAVORAVEL, JurimetriaEngine.NivelRisco.MUITO_DESFAVORAVEL);
        assertThat(risco.fatoresNegativos())
                .anyMatch(texto -> texto.contains("admissibilidade"));
        assertThat(risco.recomendacoesEstrategicas())
                .anyMatch(texto -> texto.contains("suspensão da exigibilidade"));
        assertThat(alertas)
                .anyMatch(texto -> texto.contains("Processo sigiloso"))
                .anyMatch(texto -> texto.contains("Ambiente constitucional"));
        assertThat(cenarios)
                .extracting(JurimetriaEngine.CenarioEstrategico::nome)
                .containsExactly("LITIGANCIA_ORIENTADA_A_PRECEDENTE", "ACORDO_ESTRATEGICO", "PRESSAO_RECURSAL_CONTROLADA");
    }
}
