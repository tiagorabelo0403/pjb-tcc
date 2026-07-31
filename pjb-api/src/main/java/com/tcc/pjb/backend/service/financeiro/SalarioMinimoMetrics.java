package com.tcc.pjb.backend.service.financeiro;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class SalarioMinimoMetrics {

    private static final Duration CACHE_TTL = Duration.ofSeconds(60);

    private final SalarioMinimoNacionalService salarioMinimoNacionalService;
    private final Clock clock;
    private volatile int anoCache;
    private volatile Instant cacheAtualizadoEm = Instant.EPOCH;

    public SalarioMinimoMetrics(MeterRegistry registry, SalarioMinimoNacionalService salarioMinimoNacionalService, Clock clock) {
        Objects.requireNonNull(registry);
        this.salarioMinimoNacionalService = Objects.requireNonNull(salarioMinimoNacionalService);
        this.clock = Objects.requireNonNull(clock);

        Gauge.builder("pjb.salario_minimo.ano_referencia_atual", this, SalarioMinimoMetrics::anoReferenciaCacheado)
                .description("Ano de referencia mais recente conhecido pelo motor de salario minimo")
                .register(registry);

        Gauge.builder("pjb.salario_minimo.fallback_idade_dias", this,
                        metrics -> ChronoUnit.DAYS.between(
                                LocalDate.of(metrics.anoReferenciaCacheado(), 1, 1),
                                LocalDate.now()))
                .description("Dias desde o inicio do ano de referencia mais recente conhecido")
                .register(registry);
    }

    private synchronized int anoReferenciaCacheado() {
        Instant agora = clock.instant();
        if (Duration.between(cacheAtualizadoEm, agora).compareTo(CACHE_TTL) >= 0) {
            anoCache = salarioMinimoNacionalService.anoMaisRecenteConhecido();
            cacheAtualizadoEm = agora;
        }
        return anoCache;
    }
}
