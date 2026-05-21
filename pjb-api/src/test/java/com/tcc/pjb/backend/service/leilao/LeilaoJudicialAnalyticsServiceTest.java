package com.tcc.pjb.backend.service.leilao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tcc.pjb.backend.service.leilao.LeilaoJudicialAnalyticsService.AnaliseLanceRequest;
import com.tcc.pjb.backend.service.leilao.LeilaoJudicialAnalyticsService.AnaliseLanceResponse;
import com.tcc.pjb.backend.service.leilao.LeilaoJudicialAnalyticsService.AvaliacaoBemRequest;
import com.tcc.pjb.backend.service.leilao.LeilaoJudicialAnalyticsService.AvaliacaoBemResponse;
import com.tcc.pjb.backend.service.leilao.LeilaoJudicialAnalyticsService.LanceHistorico;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LeilaoJudicialAnalyticsServiceTest {

    private final LeilaoJudicialAnalyticsService service = new LeilaoJudicialAnalyticsService();

    @ParameterizedTest(name = "tipoBem={0} conservacao={1} risco={2} → valorBase≥{3}")
    @CsvSource({
            "IMOVEL_URBANO, BOM,      0.0,  160000.00",
            "VEICULO,       EXCELENTE, 0.0,  170000.00",
            "IMOVEL_RURAL,  REGULAR,  0.10, 100000.00",
            "MAQUINARIO,    RUIM,     0.20,  60000.00"
    })
    void deveAvaliarBemAplicandoFatoresDeTipoEConservacao(String tipo, String conservacao,
                                                          double risco, BigDecimal limiteInferior) {
        AvaliacaoBemRequest request = new AvaliacaoBemRequest(tipo, new BigDecimal("200000.00"), conservacao, risco, false);

        AvaliacaoBemResponse response = service.avaliarBem(request);

        assertThat(response.valorBaseLeilao()).isGreaterThanOrEqualTo(limiteInferior);
        assertThat(response.valorBaseLeilao()).isLessThanOrEqualTo(new BigDecimal("200000.00"));
        assertThat(response.tipoBem()).isEqualTo(tipo);
    }

    @Test
    void deveCalcularValorMinimoSegundaPracaComoSessentaPorCentoDoPrimeiro() {
        AvaliacaoBemRequest request = new AvaliacaoBemRequest("IMOVEL_URBANO", new BigDecimal("500000.00"), "BOM", 0.0, false);

        AvaliacaoBemResponse response = service.avaliarBem(request);

        BigDecimal esperadoSegundaPraca = response.valorMinimoPrimeiraPraca()
                .multiply(new BigDecimal("0.60")).setScale(2, java.math.RoundingMode.HALF_UP);
        assertThat(response.valorMinimoSegundaPraca()).isEqualByComparingTo(esperadoSegundaPraca);
    }

    @Test
    void deveAdicionarFatorRiscoJuridicoElevadoQuandoRiscoAcimaDe20Porcento() {
        AvaliacaoBemRequest request = new AvaliacaoBemRequest("IMOVEL_URBANO", new BigDecimal("300000.00"), "BOM", 0.30, false);

        AvaliacaoBemResponse response = service.avaliarBem(request);

        assertThat(response.fatoresAplicados()).contains("RISCO_JURIDICO_ELEVADO");
    }

    @Test
    void deveAdicionarFatorBemOcupado() {
        AvaliacaoBemRequest request = new AvaliacaoBemRequest("IMOVEL_URBANO", new BigDecimal("300000.00"), "BOM", 0.0, true);

        AvaliacaoBemResponse response = service.avaliarBem(request);

        assertThat(response.fatoresAplicados()).contains("BEM_OCUPADO");
    }

    @Test
    void deveLancarNullPointerQuandoRequestNulo() {
        assertThatThrownBy(() -> service.avaliarBem(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void deveAceitarLanceValidoAcimaDoMinimoComIncrementoSuficiente() {
        LanceHistorico anterior = new LanceHistorico(new BigDecimal("100000.00"), "111.111.111-11", "192.168.1.1", Instant.now());
        AnaliseLanceRequest request = new AnaliseLanceRequest(
                new BigDecimal("105000.00"),
                new BigDecimal("90000.00"),
                new BigDecimal("5000.00"),
                "222.222.222-22",
                "10.0.0.5",
                true,
                List.of(anterior));

        AnaliseLanceResponse response = service.analisarLances(request);

        assertThat(response.aceito()).isTrue();
        assertThat(response.alertas()).isEmpty();
        assertThat(response.decisao()).contains("LANCE_VALIDO");
    }

    @Test
    void deveRejeitarLanceAbaixoDoPiso() {
        AnaliseLanceRequest request = new AnaliseLanceRequest(
                new BigDecimal("50000.00"),
                new BigDecimal("90000.00"),
                new BigDecimal("1000.00"),
                "111.111.111-11",
                "10.0.0.1",
                true,
                List.of());

        AnaliseLanceResponse response = service.analisarLances(request);

        assertThat(response.aceito()).isFalse();
        assertThat(response.alertas()).contains("LANCE_ABAIXO_DO_PISO");
    }

    @Test
    void deveRejeitarLanceComIncrementoInsuficiente() {
        LanceHistorico anterior = new LanceHistorico(new BigDecimal("100000.00"), "111.111.111-11", "192.168.1.1", Instant.now());
        AnaliseLanceRequest request = new AnaliseLanceRequest(
                new BigDecimal("101000.00"),
                new BigDecimal("90000.00"),
                new BigDecimal("5000.00"),
                "222.222.222-22",
                "10.0.0.5",
                true,
                List.of(anterior));

        AnaliseLanceResponse response = service.analisarLances(request);

        assertThat(response.aceito()).isFalse();
        assertThat(response.alertas()).contains("INCREMENTO_INSUFICIENTE");
    }

    @Test
    void deveRejeitarLicitanteNaoHabilitado() {
        AnaliseLanceRequest request = new AnaliseLanceRequest(
                new BigDecimal("100000.00"),
                new BigDecimal("80000.00"),
                new BigDecimal("1000.00"),
                "333.333.333-33",
                "10.0.0.9",
                false,
                List.of());

        AnaliseLanceResponse response = service.analisarLances(request);

        assertThat(response.aceito()).isFalse();
        assertThat(response.alertas()).contains("LICITANTE_NAO_HABILITADO");
    }

    @Test
    void deveAlertarConcentracaoTresLancesMesmoDocumento() {
        String doc = "444.444.444-44";
        List<LanceHistorico> historico = List.of(
                new LanceHistorico(new BigDecimal("90000.00"), doc, "10.0.0.1", Instant.now()),
                new LanceHistorico(new BigDecimal("91000.00"), doc, "10.0.0.1", Instant.now()),
                new LanceHistorico(new BigDecimal("92000.00"), doc, "10.0.0.1", Instant.now()));
        AnaliseLanceRequest request = new AnaliseLanceRequest(
                new BigDecimal("93000.00"),
                new BigDecimal("80000.00"),
                new BigDecimal("1000.00"),
                doc, "10.0.0.1",
                true,
                historico);

        AnaliseLanceResponse response = service.analisarLances(request);

        assertThat(response.alertas()).contains("CONCENTRACAO_POR_DOCUMENTO");
        assertThat(response.riscoOperacional()).isGreaterThan(0);
    }

    @Test
    void deveAlertarIpRecorrenteDoisLancesAnterioiresMesmoIp() {
        String ip = "192.168.1.50";
        List<LanceHistorico> historico = List.of(
                new LanceHistorico(new BigDecimal("80000.00"), "555.555.555-55", ip, Instant.now()),
                new LanceHistorico(new BigDecimal("82000.00"), "666.666.666-66", ip, Instant.now()));
        AnaliseLanceRequest request = new AnaliseLanceRequest(
                new BigDecimal("95000.00"),
                new BigDecimal("80000.00"),
                new BigDecimal("1000.00"),
                "777.777.777-77", ip,
                true,
                historico);

        AnaliseLanceResponse response = service.analisarLances(request);

        assertThat(response.alertas()).contains("IP_RECORRENTE_EM_LANCES");
    }
}
