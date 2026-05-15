package com.tcc.pjb.backend.service.consumidor;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ConsumidorVicioChecklistTest {

    private final ConsumidorVicioChecklistService svc = new ConsumidorVicioChecklistService();

    @Test
    void vicioEmProdutoDuravelDentroDosPrazos() {
        var input = new ConsumidorVicioChecklistService.ConsumidorVicioInput(
                "111.222.333-60",
                LocalDate.now().minusDays(10),
                null,
                ConsumidorVicioChecklistService.TipoBem.DURAVEL,
                ConsumidorVicioChecklistService.TipoVicio.PRODUTO,
                new BigDecimal("1500.00"),
                ConsumidorVicioChecklistService.CanalAquisicao.ESTABELECIMENTO_FISICO,
                false);
        var result = svc.avaliar(input);
        assertThat(result.diasDecadencialTotal()).isEqualTo(90);
        assertThat(result.diasDecadencialRestantes()).isGreaterThan(70);
        assertThat(result.pendenciasIdentificadas()).isEmpty();
        assertThat(result.direitosIndicados()).anyMatch(d -> d.descricao().contains("saneamento"));
        assertThat(result.direitosIndicados()).anyMatch(d -> d.descricao().contains("Restituição"));
        assertThat(result.sinalizacao()).contains("advogado").isNotBlank();
    }

    @Test
    void vicioEmProdutoNaoDuravelPrazo30Dias() {
        var input = new ConsumidorVicioChecklistService.ConsumidorVicioInput(
                "111.222.333-61",
                LocalDate.now().minusDays(5),
                null,
                ConsumidorVicioChecklistService.TipoBem.NAO_DURAVEL,
                ConsumidorVicioChecklistService.TipoVicio.PRODUTO,
                new BigDecimal("80.00"),
                ConsumidorVicioChecklistService.CanalAquisicao.ESTABELECIMENTO_FISICO,
                false);
        var result = svc.avaliar(input);
        assertThat(result.diasDecadencialTotal()).isEqualTo(30);
        assertThat(result.diasDecadencialRestantes()).isGreaterThan(20);
    }

    @Test
    void prazoDecadencialExpiradoGeraPendencia() {
        var input = new ConsumidorVicioChecklistService.ConsumidorVicioInput(
                "111.222.333-62",
                LocalDate.now().minusDays(100),
                null,
                ConsumidorVicioChecklistService.TipoBem.DURAVEL,
                ConsumidorVicioChecklistService.TipoVicio.PRODUTO,
                new BigDecimal("500.00"),
                ConsumidorVicioChecklistService.CanalAquisicao.ESTABELECIMENTO_FISICO,
                false);
        var result = svc.avaliar(input);
        assertThat(result.diasDecadencialRestantes()).isEqualTo(0);
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("decadencial") || p.contains("expirado"));
    }

    @Test
    void arrependimentoDentroDosPrazosCompraForaEstabelecimento() {
        var input = new ConsumidorVicioChecklistService.ConsumidorVicioInput(
                "111.222.333-63",
                LocalDate.now().minusDays(3),
                null,
                ConsumidorVicioChecklistService.TipoBem.DURAVEL,
                ConsumidorVicioChecklistService.TipoVicio.PRODUTO,
                new BigDecimal("2000.00"),
                ConsumidorVicioChecklistService.CanalAquisicao.FORA_ESTABELECIMENTO,
                false);
        var result = svc.avaliar(input);
        assertThat(result.direitoArrependimentoEligivel()).isTrue();
        assertThat(result.direitosIndicados()).anyMatch(d -> d.descricao().contains("arrependimento"));
    }

    @Test
    void arrependimentoForaDoPrazoSinalizaPendencia() {
        var input = new ConsumidorVicioChecklistService.ConsumidorVicioInput(
                "111.222.333-64",
                LocalDate.now().minusDays(10),
                null,
                ConsumidorVicioChecklistService.TipoBem.DURAVEL,
                ConsumidorVicioChecklistService.TipoVicio.PRODUTO,
                new BigDecimal("2000.00"),
                ConsumidorVicioChecklistService.CanalAquisicao.FORA_ESTABELECIMENTO,
                false);
        var result = svc.avaliar(input);
        assertThat(result.direitoArrependimentoEligivel()).isFalse();
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("7 dias") || p.contains("arrependimento"));
    }

    @Test
    void vicioServicoMapeiaDireitosCorretos() {
        var input = new ConsumidorVicioChecklistService.ConsumidorVicioInput(
                "111.222.333-65",
                LocalDate.now().minusDays(20),
                null,
                ConsumidorVicioChecklistService.TipoBem.NAO_DURAVEL,
                ConsumidorVicioChecklistService.TipoVicio.SERVICO,
                new BigDecimal("300.00"),
                ConsumidorVicioChecklistService.CanalAquisicao.ESTABELECIMENTO_FISICO,
                false);
        var result = svc.avaliar(input);
        assertThat(result.direitosIndicados()).anyMatch(d -> d.descricao().contains("Reexecução"));
        assertThat(result.direitosIndicados()).anyMatch(d -> d.descricao().contains("Restituição"));
        assertThat(result.direitosIndicados()).anyMatch(d -> d.descricao().contains("Abatimento"));
    }

    @Test
    void vicioQuantidadeMapeiaDireitosCorretos() {
        var input = new ConsumidorVicioChecklistService.ConsumidorVicioInput(
                "111.222.333-66",
                LocalDate.now().minusDays(15),
                null,
                ConsumidorVicioChecklistService.TipoBem.DURAVEL,
                ConsumidorVicioChecklistService.TipoVicio.QUANTIDADE,
                new BigDecimal("450.00"),
                ConsumidorVicioChecklistService.CanalAquisicao.ESTABELECIMENTO_FISICO,
                false);
        var result = svc.avaliar(input);
        assertThat(result.direitosIndicados()).anyMatch(d -> d.descricao().contains("Complementação"));
        assertThat(result.direitosIndicados()).anyMatch(d -> d.descricao().contains("Substituição"));
    }

    @Test
    void vicioOcultoContaPrazoDesdeConstatacao() {
        var input = new ConsumidorVicioChecklistService.ConsumidorVicioInput(
                "111.222.333-67",
                LocalDate.now().minusYears(1),
                LocalDate.now().minusDays(15),
                ConsumidorVicioChecklistService.TipoBem.DURAVEL,
                ConsumidorVicioChecklistService.TipoVicio.PRODUTO,
                new BigDecimal("800.00"),
                ConsumidorVicioChecklistService.CanalAquisicao.ESTABELECIMENTO_FISICO,
                true);
        var result = svc.avaliar(input);
        assertThat(result.diasDecadencialRestantes()).isGreaterThan(70);
        assertThat(result.requisitosVerificados()).anyMatch(r -> r.contains("oculto") || r.contains("manifestação"));
    }

    @Test
    void prescricaoDanosProximaSinalizaPendencia() {
        var input = new ConsumidorVicioChecklistService.ConsumidorVicioInput(
                "111.222.333-68",
                LocalDate.now().minusYears(4).minusMonths(6),
                null,
                ConsumidorVicioChecklistService.TipoBem.DURAVEL,
                ConsumidorVicioChecklistService.TipoVicio.PRODUTO,
                new BigDecimal("1200.00"),
                ConsumidorVicioChecklistService.CanalAquisicao.ESTABELECIMENTO_FISICO,
                false);
        var result = svc.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).anyMatch(p ->
                p.contains("prescricional") || p.contains("prescrição") || p.contains("5 anos"));
    }

    @Test
    void enumTipoBemTemDuasModalidades() {
        assertThat(ConsumidorVicioChecklistService.TipoBem.values()).hasSize(2);
    }

    @Test
    void enumTipoVicioTemTresModalidades() {
        assertThat(ConsumidorVicioChecklistService.TipoVicio.values()).hasSize(3);
    }
}
