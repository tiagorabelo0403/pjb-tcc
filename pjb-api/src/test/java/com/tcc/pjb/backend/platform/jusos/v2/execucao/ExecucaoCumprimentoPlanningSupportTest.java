package com.tcc.pjb.backend.platform.jusos.v2.execucao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.entity.Jurisdicao;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine;
import com.tcc.pjb.backend.platform.jusos.v2.rules.NationalRulePackEngine;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExecucaoCumprimentoPlanningSupportTest {

    private NationalPrazoEngine prazoEngine;
    private NationalRulePackEngine rulePackEngine;
    private ExecucaoCumprimentoPlanningSupport support;

    @BeforeEach
    void setUp() {
        prazoEngine = mock(NationalPrazoEngine.class);
        rulePackEngine = mock(NationalRulePackEngine.class);
        when(rulePackEngine.aplicar(any())).thenReturn(new NationalRulePackEngine.ResultadoRegras(List.of(), List.of("alerta-regra"), List.of(), false, 0));
        when(prazoEngine.calcularPorRamo(any(), any(), any(), any(), any())).thenReturn(new NationalPrazoEngine.PrazoCalculado(
                LocalDate.now(),
                LocalDate.now().plusDays(3),
                3,
                3,
                NationalPrazoEngine.TipoPrazo.PRAZO_GENERICO,
                RamoDireito.FAMILIA,
                GrauJurisdicao.PRIMEIRO_GRAU,
                false,
                List.of(),
                "fundamento"
        ));
        support = new ExecucaoCumprimentoPlanningSupport(prazoEngine, rulePackEngine);
    }

    @Test
    void deveMontarPlanoAlimentarComDescontoEmFolhaEUrgencia() {
        Processo processo = processoFamilia();

        ExecucaoCumprimentoEngine.PlanoExecucao plano = support.montarPlano(
                processo,
                ExecucaoCumprimentoEngine.TituloExecutivo.SENTENCA_CONDENATORIA,
                new BigDecimal("45000.00")
        );

        assertThat(plano.compliance().naturezaAlimentar()).isTrue();
        assertThat(plano.meiosOrdenados()).contains(ExecucaoCumprimentoEngine.MeioExpropriatorio.DESCONTO_FOLHA_ALIMENTOS);
        assertThat(plano.alertas()).contains("alerta-regra");
        assertThat(plano.checklist()).anyMatch(item -> item.contains("urgência material"));
    }

    @Test
    void deveResolverPrazoEmHorasParaFluxoTrabalhista() {
        Processo processo = new Processo();
        processo.setRamoDireito(RamoDireito.TRABALHISTA);

        ExecucaoPrazoResposta prazo = support.resolverPrazoResposta(
                processo,
                ExecucaoCumprimentoEngine.TituloExecutivo.CHEQUE_NOTA_PROMISSORIA
        );

        assertThat(prazo.numero()).isEqualTo(48);
        assertThat(prazo.emHoras()).isTrue();
        assertThat(prazo.integradoAoMotor()).isFalse();
    }

    private Processo processoFamilia() {
        Processo processo = new Processo();
        processo.setId(10L);
        processo.setNumeroUnificado("0001234-55.2026.8.06.0001");
        processo.setRamoDireito(RamoDireito.FAMILIA);
        processo.setValorCausa(new BigDecimal("32000.00"));
        processo.setClasseProcessual("Cumprimento de sentença");
        processo.setAssunto("Alimentos");
        Jurisdicao jurisdicao = new Jurisdicao();
        jurisdicao.setCodigo("TJCE");
        jurisdicao.setGrau(GrauJurisdicao.PRIMEIRO_GRAU);
        processo.setJurisdicao(jurisdicao);
        return processo;
    }
}
