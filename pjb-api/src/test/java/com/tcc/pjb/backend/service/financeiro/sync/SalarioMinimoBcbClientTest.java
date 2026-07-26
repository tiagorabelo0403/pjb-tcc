package com.tcc.pjb.backend.service.financeiro.sync;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class SalarioMinimoBcbClientTest {

    private final SalarioMinimoBcbClient client = new SalarioMinimoBcbClient(
            RestClient.builder().build(),
            new ObjectMapper(),
            "https://api.bcb.gov.br/dados/serie/bcdata.sgs.1619/dados/ultimos/1?formato=json");

    @Test
    void payloadValidoDoBcbEhParseadoParaSnapshot() {
        String body = "[{\"data\":\"01/01/2026\",\"valor\":\"1621.00\"}]";

        Optional<SalarioMinimoBcbClient.SnapshotSalarioMinimo> snapshot = client.parse(body);

        assertThat(snapshot).isPresent();
        assertThat(snapshot.get().dataReferencia()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(snapshot.get().valorMensal()).isEqualByComparingTo(new BigDecimal("1621.00"));
    }

    @Test
    void quandoBcbRetornaMultiplasEntradasUsaAUltima() {
        String body = "[{\"data\":\"01/01/2024\",\"valor\":\"1412.00\"},"
                + "{\"data\":\"01/01/2025\",\"valor\":\"1518.00\"},"
                + "{\"data\":\"01/01/2026\",\"valor\":\"1621.00\"}]";

        Optional<SalarioMinimoBcbClient.SnapshotSalarioMinimo> snapshot = client.parse(body);

        assertThat(snapshot).isPresent();
        assertThat(snapshot.get().dataReferencia()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(snapshot.get().valorMensal()).isEqualByComparingTo(new BigDecimal("1621.00"));
    }

    @Test
    void payloadVazioRetornaOptionalEmpty() {
        assertThat(client.parse("[]")).isEmpty();
    }

    @Test
    void payloadNuloRetornaOptionalEmpty() {
        assertThat(client.parse(null)).isEmpty();
    }

    @Test
    void payloadEmBrancoRetornaOptionalEmpty() {
        assertThat(client.parse("   ")).isEmpty();
    }

    @Test
    void payloadNaoArrayRetornaOptionalEmpty() {
        assertThat(client.parse("{\"data\":\"01/01/2026\",\"valor\":\"1621.00\"}")).isEmpty();
    }

    @Test
    void payloadComCampoDataAusenteRetornaOptionalEmpty() {
        assertThat(client.parse("[{\"valor\":\"1621.00\"}]")).isEmpty();
    }

    @Test
    void payloadComCampoValorAusenteRetornaOptionalEmpty() {
        assertThat(client.parse("[{\"data\":\"01/01/2026\"}]")).isEmpty();
    }

    @Test
    void payloadComDataInvalidaRetornaOptionalEmpty() {
        assertThat(client.parse("[{\"data\":\"2026-01-01\",\"valor\":\"1621.00\"}]")).isEmpty();
    }

    @Test
    void payloadComValorNegativoRetornaOptionalEmpty() {
        assertThat(client.parse("[{\"data\":\"01/01/2026\",\"valor\":\"-100.00\"}]")).isEmpty();
    }

    @Test
    void payloadComValorZeroRetornaOptionalEmpty() {
        assertThat(client.parse("[{\"data\":\"01/01/2026\",\"valor\":\"0.00\"}]")).isEmpty();
    }

    @Test
    void payloadJsonInvalidoRetornaOptionalEmpty() {
        assertThat(client.parse("nao e json")).isEmpty();
    }
}
