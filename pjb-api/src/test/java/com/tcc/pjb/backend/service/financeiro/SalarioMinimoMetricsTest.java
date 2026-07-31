package com.tcc.pjb.backend.service.financeiro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.support.MutableClock;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class SalarioMinimoMetricsTest {

    @Test
    void gaugeDeAnoReferenciaAtualRefleteOServicoCanonico() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        SalarioMinimoNacionalService service = mock(SalarioMinimoNacionalService.class);
        when(service.anoMaisRecenteConhecido()).thenReturn(2026);
        new SalarioMinimoMetrics(registry, service, Clock.systemUTC());

        double valor = registry.get("pjb.salario_minimo.ano_referencia_atual").gauge().value();

        assertThat(valor).isEqualTo(2026.0);
    }

    @Test
    void gaugeDeIdadeDoFallbackEmDiasBateComDiasDesdeInicioDoAno() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        SalarioMinimoNacionalService service = mock(SalarioMinimoNacionalService.class);
        int anoPassado = LocalDate.now().getYear() - 1;
        when(service.anoMaisRecenteConhecido()).thenReturn(anoPassado);
        new SalarioMinimoMetrics(registry, service, Clock.systemUTC());

        long diasEsperados = java.time.temporal.ChronoUnit.DAYS.between(
                LocalDate.of(anoPassado, 1, 1), LocalDate.now());

        double valor = registry.get("pjb.salario_minimo.fallback_idade_dias").gauge().value();

        assertThat(valor).isEqualTo((double) diasEsperados);
    }

    @Test
    void asDuasGaugesCompartilhamUmaUnicaLeituraCacheadaPorScrape() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        SalarioMinimoNacionalService service = mock(SalarioMinimoNacionalService.class);
        when(service.anoMaisRecenteConhecido()).thenReturn(2026);
        new SalarioMinimoMetrics(registry, service, Clock.systemUTC());

        registry.get("pjb.salario_minimo.ano_referencia_atual").gauge().value();
        registry.get("pjb.salario_minimo.fallback_idade_dias").gauge().value();
        registry.get("pjb.salario_minimo.ano_referencia_atual").gauge().value();
        registry.get("pjb.salario_minimo.fallback_idade_dias").gauge().value();

        verify(service, times(1)).anoMaisRecenteConhecido();
    }

    @Test
    void leituraAntesDoTtlNaoChamaServicoDeNovoEAposOTtlChama() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        SalarioMinimoNacionalService service = mock(SalarioMinimoNacionalService.class);
        when(service.anoMaisRecenteConhecido()).thenReturn(2026);
        MutableClock clock = new MutableClock(Instant.parse("2026-07-31T00:00:00Z"));
        SalarioMinimoMetrics metrics = new SalarioMinimoMetrics(registry, service, clock);

        registry.get("pjb.salario_minimo.ano_referencia_atual").gauge().value();
        clock.advance(Duration.ofSeconds(59));
        registry.get("pjb.salario_minimo.ano_referencia_atual").gauge().value();

        verify(service, times(1)).anoMaisRecenteConhecido();

        clock.advance(Duration.ofSeconds(2));
        registry.get("pjb.salario_minimo.ano_referencia_atual").gauge().value();

        verify(service, times(2)).anoMaisRecenteConhecido();
    }
}
