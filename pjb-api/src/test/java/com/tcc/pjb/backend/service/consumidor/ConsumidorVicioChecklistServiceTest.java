package com.tcc.pjb.backend.service.consumidor;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.service.consumidor.ConsumidorVicioChecklistService.CanalAquisicao;
import com.tcc.pjb.backend.service.consumidor.ConsumidorVicioChecklistService.ConsumidorVicioInput;
import com.tcc.pjb.backend.service.consumidor.ConsumidorVicioChecklistService.ConsumidorVicioResult;
import com.tcc.pjb.backend.service.consumidor.ConsumidorVicioChecklistService.TipoBem;
import com.tcc.pjb.backend.service.consumidor.ConsumidorVicioChecklistService.TipoVicio;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ConsumidorVicioChecklistServiceTest {

    private final ConsumidorVicioChecklistService service = new ConsumidorVicioChecklistService();

    @ParameterizedTest(name = "bem={0} → prazoTotal={1}")
    @CsvSource({
            "DURAVEL,     90",
            "NAO_DURAVEL, 30"
    })
    void deveRetornarPrazoDecadencialCorreto(TipoBem tipoBem, int prazoEsperado) {
        ConsumidorVicioInput input = new ConsumidorVicioInput(
                "111.111.111-11", LocalDate.now().minusDays(5), LocalDate.now().minusDays(2),
                tipoBem, TipoVicio.PRODUTO, new BigDecimal("500.00"),
                CanalAquisicao.ESTABELECIMENTO_FISICO, false);

        ConsumidorVicioResult result = service.avaliar(input);

        assertThat(result.diasDecadencialTotal()).isEqualTo(prazoEsperado);
    }

    @Test
    void deveIndicarArrependimentoParaCompraForaEstabelecimentoDentroP7Dias() {
        ConsumidorVicioInput input = new ConsumidorVicioInput(
                "111.111.111-11", LocalDate.now().minusDays(3), LocalDate.now().minusDays(1),
                TipoBem.DURAVEL, TipoVicio.PRODUTO, new BigDecimal("1200.00"),
                CanalAquisicao.FORA_ESTABELECIMENTO, false);

        ConsumidorVicioResult result = service.avaliar(input);

        assertThat(result.direitoArrependimentoEligivel()).isTrue();
        assertThat(result.direitosIndicados()).anyMatch(d -> d.fundamentoLegal().contains("49"));
    }

    @Test
    void deveNaoIndicarArrependimentoParaCompraEmEstabelecimento() {
        ConsumidorVicioInput input = new ConsumidorVicioInput(
                "111.111.111-11", LocalDate.now().minusDays(10), LocalDate.now().minusDays(5),
                TipoBem.DURAVEL, TipoVicio.PRODUTO, new BigDecimal("800.00"),
                CanalAquisicao.ESTABELECIMENTO_FISICO, false);

        ConsumidorVicioResult result = service.avaliar(input);

        assertThat(result.direitoArrependimentoEligivel()).isFalse();
    }

    @Test
    void deveAdicionarPendenciaQuandoPrazoDecadencialExpirado() {
        LocalDate aquisicaoAntiga = LocalDate.now().minusDays(100);
        ConsumidorVicioInput input = new ConsumidorVicioInput(
                "111.111.111-11", aquisicaoAntiga, aquisicaoAntiga.plusDays(2),
                TipoBem.DURAVEL, TipoVicio.PRODUTO, new BigDecimal("300.00"),
                CanalAquisicao.ESTABELECIMENTO_FISICO, false);

        ConsumidorVicioResult result = service.avaliar(input);

        assertThat(result.diasDecadencialRestantes()).isEqualTo(0);
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("expirado"));
    }

    @Test
    void deveMapearDireitosParaVicioServico() {
        ConsumidorVicioInput input = new ConsumidorVicioInput(
                "111.111.111-11", LocalDate.now().minusDays(10), LocalDate.now().minusDays(5),
                TipoBem.DURAVEL, TipoVicio.SERVICO, new BigDecimal("400.00"),
                CanalAquisicao.ESTABELECIMENTO_FISICO, false);

        ConsumidorVicioResult result = service.avaliar(input);

        assertThat(result.direitosIndicados()).anyMatch(d -> d.fundamentoLegal().contains("art. 20"));
    }

    @Test
    void deveMapearDireitosParaVicioQuantidade() {
        ConsumidorVicioInput input = new ConsumidorVicioInput(
                "111.111.111-11", LocalDate.now().minusDays(15), LocalDate.now().minusDays(10),
                TipoBem.NAO_DURAVEL, TipoVicio.QUANTIDADE, new BigDecimal("50.00"),
                CanalAquisicao.ESTABELECIMENTO_FISICO, false);

        ConsumidorVicioResult result = service.avaliar(input);

        assertThat(result.direitosIndicados()).anyMatch(d -> d.fundamentoLegal().contains("art. 19"));
    }

    @Test
    void deveUsarDataConstatacaoParaVicioOculto() {
        LocalDate aquisicao = LocalDate.now().minusDays(200);
        LocalDate constatacao = LocalDate.now().minusDays(10);
        ConsumidorVicioInput input = new ConsumidorVicioInput(
                "111.111.111-11", aquisicao, constatacao,
                TipoBem.DURAVEL, TipoVicio.PRODUTO, new BigDecimal("2000.00"),
                CanalAquisicao.ESTABELECIMENTO_FISICO, true);

        ConsumidorVicioResult result = service.avaliar(input);

        assertThat(result.diasDecadencialRestantes()).isGreaterThan(0);
        assertThat(result.requisitosVerificados()).anyMatch(v -> v.contains("oculto"));
    }

    @Test
    void deveSinalizarComPendenciasQuandoPrazoProximo() {
        ConsumidorVicioInput input = new ConsumidorVicioInput(
                "111.111.111-11", LocalDate.now().minusDays(27), LocalDate.now().minusDays(25),
                TipoBem.NAO_DURAVEL, TipoVicio.PRODUTO, new BigDecimal("100.00"),
                CanalAquisicao.ESTABELECIMENTO_FISICO, false);

        ConsumidorVicioResult result = service.avaliar(input);

        assertThat(result.sinalizacao()).contains("Pendências");
    }
}
