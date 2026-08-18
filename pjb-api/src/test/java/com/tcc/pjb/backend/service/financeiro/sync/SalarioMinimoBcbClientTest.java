package com.tcc.pjb.backend.service.financeiro.sync;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SalarioMinimoBcbClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void fallbackDeCircuitBreakerRetornaOptionalEmptySemLancarExcecao() {
        SalarioMinimoBcbClient client = new SalarioMinimoBcbClient(
                HttpClient.newHttpClient(), objectMapper, "https://api.bcb.gov.br/dados/serie/bcdata.sgs.1619/dados/ultimos/1?formato=json");

        Optional<SalarioMinimoBcbClient.SnapshotSalarioMinimo> resultado =
                client.buscarUltimoValorFallback(new RuntimeException("BCB indisponivel"));

        assertThat(resultado).isEmpty();
    }

    @Test
    void payloadValidoDoBcbEhParseadoParaSnapshot() {
        String body = "[{\"data\":\"01/01/2026\",\"valor\":\"1621.00\"}]";

        Optional<SalarioMinimoBcbClient.SnapshotSalarioMinimo> snapshot = SalarioMinimoBcbClient.parse(objectMapper,body);

        assertThat(snapshot).isPresent();
        assertThat(snapshot.get().dataReferencia()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(snapshot.get().valorMensal()).isEqualByComparingTo(new BigDecimal("1621.00"));
    }

    @Test
    void quandoBcbRetornaMultiplasEntradasUsaAUltima() {
        String body = "[{\"data\":\"01/01/2024\",\"valor\":\"1412.00\"},"
                + "{\"data\":\"01/01/2025\",\"valor\":\"1518.00\"},"
                + "{\"data\":\"01/01/2026\",\"valor\":\"1621.00\"}]";

        Optional<SalarioMinimoBcbClient.SnapshotSalarioMinimo> snapshot = SalarioMinimoBcbClient.parse(objectMapper,body);

        assertThat(snapshot).isPresent();
        assertThat(snapshot.get().dataReferencia()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(snapshot.get().valorMensal()).isEqualByComparingTo(new BigDecimal("1621.00"));
    }

    @Test
    void payloadVazioRetornaOptionalEmpty() {
        assertThat(SalarioMinimoBcbClient.parse(objectMapper,"[]")).isEmpty();
    }

    @Test
    void payloadNuloRetornaOptionalEmpty() {
        assertThat(SalarioMinimoBcbClient.parse(objectMapper,null)).isEmpty();
    }

    @Test
    void payloadEmBrancoRetornaOptionalEmpty() {
        assertThat(SalarioMinimoBcbClient.parse(objectMapper,"   ")).isEmpty();
    }

    @Test
    void payloadNaoArrayRetornaOptionalEmpty() {
        assertThat(SalarioMinimoBcbClient.parse(objectMapper,"{\"data\":\"01/01/2026\",\"valor\":\"1621.00\"}")).isEmpty();
    }

    @Test
    void payloadComCampoDataAusenteRetornaOptionalEmpty() {
        assertThat(SalarioMinimoBcbClient.parse(objectMapper,"[{\"valor\":\"1621.00\"}]")).isEmpty();
    }

    @Test
    void payloadComCampoValorAusenteRetornaOptionalEmpty() {
        assertThat(SalarioMinimoBcbClient.parse(objectMapper,"[{\"data\":\"01/01/2026\"}]")).isEmpty();
    }

    @Test
    void payloadComDataInvalidaRetornaOptionalEmpty() {
        assertThat(SalarioMinimoBcbClient.parse(objectMapper,"[{\"data\":\"2026-01-01\",\"valor\":\"1621.00\"}]")).isEmpty();
    }

    @Test
    void payloadComValorNegativoRetornaOptionalEmpty() {
        assertThat(SalarioMinimoBcbClient.parse(objectMapper,"[{\"data\":\"01/01/2026\",\"valor\":\"-100.00\"}]")).isEmpty();
    }

    @Test
    void payloadComValorZeroRetornaOptionalEmpty() {
        assertThat(SalarioMinimoBcbClient.parse(objectMapper,"[{\"data\":\"01/01/2026\",\"valor\":\"0.00\"}]")).isEmpty();
    }

    @Test
    void payloadJsonInvalidoRetornaOptionalEmpty() {
        assertThat(SalarioMinimoBcbClient.parse(objectMapper,"nao e json")).isEmpty();
    }
}
