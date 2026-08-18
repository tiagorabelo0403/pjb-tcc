package com.tcc.pjb.backend.service.contrato;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.service.contrato.ContratoVicioResolucaoChecklistService.ContratoVicioResolucaoInput;
import com.tcc.pjb.backend.service.contrato.ContratoVicioResolucaoChecklistService.ContratoVicioResolucaoResult;
import com.tcc.pjb.backend.service.contrato.ContratoVicioResolucaoChecklistService.GrauInvalidade;
import com.tcc.pjb.backend.service.contrato.ContratoVicioResolucaoChecklistService.MotivoResolucao;
import com.tcc.pjb.backend.service.contrato.ContratoVicioResolucaoChecklistService.VicioVontade;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ContratoVicioResolucaoChecklistServiceTest {

    private final ContratoVicioResolucaoChecklistService service =
            new ContratoVicioResolucaoChecklistService();

    @Test
    void deveRetornarAnulavelParaVicioDolo() {
        ContratoVicioResolucaoInput input = new ContratoVicioResolucaoInput(
                VicioVontade.DOLO, MotivoResolucao.INADIMPLEMENTO_ABSOLUTO,
                LocalDate.now().minusYears(1), LocalDate.now().minusMonths(6),
                false, 0, 50000, false, false);

        ContratoVicioResolucaoResult result = service.avaliar(input);

        assertThat(result.grauInvalidade()).isEqualTo(GrauInvalidade.ANULAVEL);
        assertThat(result.prescricaoAnulacaoExpirada()).isFalse();
    }

    @Test
    void deveDetectarPrescricaoAnulacaoExpirada() {
        ContratoVicioResolucaoInput input = new ContratoVicioResolucaoInput(
                VicioVontade.ERRO, null,
                LocalDate.now().minusYears(6), LocalDate.now().minusYears(5),
                false, 0, 20000, false, false);

        ContratoVicioResolucaoResult result = service.avaliar(input);

        assertThat(result.prescricaoAnulacaoExpirada()).isTrue();
        assertThat(result.diasParaPrescricaoAnulacao()).isEqualTo(0);
        assertThat(result.sinalizacao()).contains("expirado");
    }

    @Test
    void deveRecomendarReducaoClausulaPenalExcessiva() {
        ContratoVicioResolucaoInput input = new ContratoVicioResolucaoInput(
                null, MotivoResolucao.INADIMPLEMENTO_ABSOLUTO,
                LocalDate.now().minusYears(1), null,
                true, 60000, 50000, false, false);

        ContratoVicioResolucaoResult result = service.avaliar(input);

        assertThat(result.clausulaPenalReducaoRecomendada()).isTrue();
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("art. 413"));
    }

    @Test
    void deveAdicionarOrientacaoCumprimentoSubstancial() {
        ContratoVicioResolucaoInput input = new ContratoVicioResolucaoInput(
                null, MotivoResolucao.INADIMPLEMENTO_PARCIAL,
                LocalDate.now().minusYears(1), null,
                false, 0, 30000, true, true);

        ContratoVicioResolucaoResult result = service.avaliar(input);

        assertThat(result.orientacoes()).anyMatch(o -> o.contains("substancial"));
    }

    @Test
    void deveAdicionarOrientacaoFraudeCredores() {
        ContratoVicioResolucaoInput input = new ContratoVicioResolucaoInput(
                VicioVontade.FRAUDE_CONTRA_CREDORES, null,
                LocalDate.now().minusYears(1), LocalDate.now().minusMonths(3),
                false, 0, 100000, false, false);

        ContratoVicioResolucaoResult result = service.avaliar(input);

        assertThat(result.orientacoes()).anyMatch(o -> o.contains("pauliana"));
    }

    @Test
    void deveAdicionarOrientacaoClausulaResolutivaExpressa() {
        ContratoVicioResolucaoInput input = new ContratoVicioResolucaoInput(
                null, MotivoResolucao.CLAUSULA_RESOLUTIVA_EXPRESSA,
                LocalDate.now().minusYears(1), null,
                false, 0, 40000, false, false);

        ContratoVicioResolucaoResult result = service.avaliar(input);

        assertThat(result.orientacoes()).anyMatch(o -> o.contains("art. 474"));
    }

    @ParameterizedTest(name = "vicio={0}")
    @EnumSource(VicioVontade.class)
    void deveRetornarAnulavelParaTodosViciosVontade(VicioVontade vicio) {
        ContratoVicioResolucaoInput input = new ContratoVicioResolucaoInput(
                vicio, null,
                LocalDate.now().minusYears(1), LocalDate.now().minusMonths(1),
                false, 0, 10000, false, false);

        ContratoVicioResolucaoResult result = service.avaliar(input);

        assertThat(result.grauInvalidade()).isEqualTo(GrauInvalidade.ANULAVEL);
    }

    @Test
    void deveAdicionarPendenciaDataCienciaNaoInformada() {
        ContratoVicioResolucaoInput input = new ContratoVicioResolucaoInput(
                VicioVontade.COACAO, null,
                LocalDate.now().minusYears(1), null,
                false, 0, 25000, false, false);

        ContratoVicioResolucaoResult result = service.avaliar(input);

        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("data da ciência"));
    }
}
