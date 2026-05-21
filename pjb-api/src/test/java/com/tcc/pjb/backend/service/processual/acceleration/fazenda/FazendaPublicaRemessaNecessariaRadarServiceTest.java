package com.tcc.pjb.backend.service.processual.acceleration.fazenda;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.service.processual.acceleration.fazenda.FazendaPublicaRemessaNecessariaRadarService.RemessaInput;
import com.tcc.pjb.backend.service.processual.acceleration.fazenda.FazendaPublicaRemessaNecessariaRadarService.RemessaResultado;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FazendaPublicaRemessaNecessariaRadarServiceTest {

    private final FazendaPublicaRemessaNecessariaRadarService service =
            new FazendaPublicaRemessaNecessariaRadarService();

    @ParameterizedTest(name = "ente={0} valor={1} → exige={2}")
    @CsvSource({
            "UNIAO_FEDERAL,  1100000.00, true",
            "UNIAO_FEDERAL,   900000.00, false",
            "ESTADO_SP,       600000.00, true",
            "ESTADO_SP,       400000.00, false"
    })
    void deveAvaliarRemessaNecessariaConformalLimite(String ente, BigDecimal valor, boolean esperado) {
        RemessaInput input = new RemessaInput(UUID.randomUUID(), "0001", valor, ente, false);

        RemessaResultado result = service.avaliar(input);

        assertThat(result.exigeRemessa()).isEqualTo(esperado);
        assertThat(result.fundamento()).contains("496");
    }

    @Test
    void deveAplicarLimiteMunicipioPequenoQuando100Mil() {
        RemessaInput input = new RemessaInput(
                UUID.randomUUID(), "0002", new BigDecimal("150000.00"),
                "MUNICIPIO_INTERIOR", true);

        RemessaResultado result = service.avaliar(input);

        assertThat(result.exigeRemessa()).isTrue();
    }

    @Test
    void deveDispensarRemessaMunicipioPequenoAbaixoDe100Mil() {
        RemessaInput input = new RemessaInput(
                UUID.randomUUID(), "0003", new BigDecimal("80000.00"),
                "MUNICIPIO_INTERIOR", true);

        RemessaResultado result = service.avaliar(input);

        assertThat(result.exigeRemessa()).isFalse();
        assertThat(result.mensagem()).contains("dispensada");
    }

    @Test
    void deveRadarFiltrarApenasQueExigemRemessa() {
        List<RemessaInput> processos = List.of(
                new RemessaInput(UUID.randomUUID(), "A", new BigDecimal("1200000.00"), "UNIAO_FEDERAL", false),
                new RemessaInput(UUID.randomUUID(), "B", new BigDecimal("500000.00"), "UNIAO_FEDERAL", false),
                new RemessaInput(UUID.randomUUID(), "C", new BigDecimal("600000.00"), "ESTADO_RS", false));

        List<RemessaResultado> resultado = service.radar(processos);

        assertThat(resultado).hasSize(2);
        assertThat(resultado).allMatch(RemessaResultado::exigeRemessa);
    }

    @Test
    void devePreservarProcessoIdNoResultado() {
        UUID id = UUID.randomUUID();
        RemessaInput input = new RemessaInput(id, "0004", new BigDecimal("2000000.00"), "UNIAO_FEDERAL", false);

        RemessaResultado result = service.avaliar(input);

        assertThat(result.processoId()).isEqualTo(id);
    }
}
