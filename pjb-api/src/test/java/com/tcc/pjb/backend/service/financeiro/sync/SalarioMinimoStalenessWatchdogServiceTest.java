package com.tcc.pjb.backend.service.financeiro.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.service.financeiro.SalarioMinimoNacionalService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class SalarioMinimoStalenessWatchdogServiceTest {

    private static final String GAUGE = "pjb.salario_minimo.defasagem_anos";

    private SalarioMinimoStalenessWatchdogService comAnoConhecido(int ano, int limiar, SimpleMeterRegistry registry) {
        SalarioMinimoNacionalService service = mock(SalarioMinimoNacionalService.class);
        when(service.anoMaisRecenteConhecido()).thenReturn(ano);
        return new SalarioMinimoStalenessWatchdogService(service, limiar, registry);
    }

    private double gauge(SimpleMeterRegistry registry) {
        return registry.find(GAUGE).gauge().value();
    }

    @Test
    void gaugeComecaEmMenosUmAntesDaPrimeiraExecucao() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        comAnoConhecido(LocalDate.now().getYear(), 1, registry);

        assertThat(gauge(registry))
                .as("-1 distingue 'watchdog ainda nao rodou' de 'defasagem zero'")
                .isEqualTo(-1.0);
    }

    @Test
    void gaugeExpoeADefasagemMesmoQuandoElaNaoDisparaOAlerta() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        var watchdog = comAnoConhecido(LocalDate.now().getYear() - 1, 1, registry);

        boolean defasado = watchdog.verificarDefasagem();

        assertThat(defasado)
                .as("limiar > 1 tolera um ano de defasagem por decisao documentada")
                .isFalse();
        assertThat(gauge(registry))
                .as("a defasagem tolerada precisa ser visivel, senao um ano inteiro passa sem sinal")
                .isEqualTo(1.0);
    }

    @Test
    void defasagemAcimaDoLimiarEhReportadaEExposta() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        var watchdog = comAnoConhecido(LocalDate.now().getYear() - 3, 1, registry);

        assertThat(watchdog.verificarDefasagem()).isTrue();
        assertThat(gauge(registry)).isEqualTo(3.0);
    }

    @Test
    void semDefasagemOGaugeVaiAZero() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        var watchdog = comAnoConhecido(LocalDate.now().getYear(), 1, registry);

        assertThat(watchdog.verificarDefasagem()).isFalse();
        assertThat(gauge(registry)).isZero();
    }
}
