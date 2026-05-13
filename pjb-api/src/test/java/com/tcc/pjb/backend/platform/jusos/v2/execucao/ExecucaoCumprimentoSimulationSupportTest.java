package com.tcc.pjb.backend.platform.jusos.v2.execucao;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExecucaoCumprimentoSimulationSupportTest {

    private ExecucaoCumprimentoSimulationSupport support;

    @BeforeEach
    void setUp() {
        support = new ExecucaoCumprimentoSimulationSupport();
    }

    @Test
    void deveSimularPenhoraAlimentarComScoreElevado() {
        Processo processo = new Processo();
        processo.setRamoDireito(RamoDireito.FAMILIA);
        processo.setValorCausa(new BigDecimal("12000.00"));

        ExecucaoCumprimentoEngine.ResultadoPenhora resultado = support.simularPenhora(
                processo,
                12L,
                ExecucaoCumprimentoEngine.MeioExpropriatorio.DESCONTO_FOLHA_ALIMENTOS,
                new BigDecimal("10000.00")
        );

        assertThat(resultado.scoreRecuperacao()).isGreaterThanOrEqualTo(88);
        assertThat(resultado.bensLocalizados()).anyMatch(item -> item.contains("folha"));
        assertThat(resultado.provasRecomendadas()).anyMatch(item -> item.contains("Fonte pagadora"));
    }

    @Test
    void deveGerarPainelComGargaloEOportunidade() {
        Processo processo = new Processo();
        processo.setId(99L);
        processo.setNumeroUnificado("0009999-11.2026.8.06.0001");
        processo.setRamoDireito(RamoDireito.CIVIL);

        ExecucaoCumprimentoEngine.PlanoExecucao plano = new ExecucaoCumprimentoEngine.PlanoExecucao(
                99L,
                ExecucaoCumprimentoEngine.TituloExecutivo.SENTENCA_CONDENATORIA,
                new BigDecimal("10000.00"),
                new BigDecimal("10600.00"),
                new BigDecimal("200.00"),
                new BigDecimal("1000.00"),
                List.of(ExecucaoCumprimentoEngine.MeioExpropriatorio.SISBAJUD_BLOQUEIO_CONTA),
                List.of("Primeira ação"),
                List.of(),
                "CPC",
                true,
                15,
                false,
                FaseProcessual.CUMPRIMENTO_SENTENCA,
                StatusProcesso.CUMPRIMENTO_SENTENCA,
                new NationalPrazoEngine.PrazoCalculado(LocalDate.now(), LocalDate.now().plusDays(2), 2, 2, NationalPrazoEngine.TipoPrazo.IMPUGNACAO_CUMPRIMENTO, RamoDireito.CIVIL, null, false, List.of(), "fundamento"),
                new ExecucaoCumprimentoEngine.ComplianceExecucao(true, true, false, true, false, true, List.of(), List.of(), List.of()),
                new ExecucaoCumprimentoEngine.MatrizExpropriacao(78, 60, 22, new BigDecimal("7000.00"), List.of(), List.of(), List.of(), List.of()),
                List.of(),
                List.of()
        );
        List<ExecucaoCumprimentoEngine.MedidaExpropriatoria> medidas = List.of(
                new ExecucaoCumprimentoEngine.MedidaExpropriatoria(null, 99L, ExecucaoCumprimentoEngine.MeioExpropriatorio.SISBAJUD_BLOQUEIO_CONTA, ExecucaoCumprimentoEngine.StatusMedidaExpropriatoria.REQUERIDA, new BigDecimal("10000.00"), new BigDecimal("0.00"), null, null, null, null, null, null)
        );

        ExecucaoCumprimentoEngine.PainelExecucao painel = support.gerarPainel(processo, plano, medidas);

        assertThat(painel.gargalos()).anyMatch(item -> item.contains("pendentes"));
        assertThat(painel.oportunidades()).anyMatch(item -> item.contains("Recuperabilidade elevada"));
        assertThat(painel.prazoCritico()).isTrue();
    }
}
