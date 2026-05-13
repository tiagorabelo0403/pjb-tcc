package com.tcc.pjb.backend.configs.time;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.auditing.DateTimeProvider;

@Configuration
public class TimeConfig {

    @Bean(name = {"clock", "pjbClock"})
    @Primary
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public ZoneId pjbLegalZone(@Value("${pjb.prazos.timezone:America/Sao_Paulo}") String zoneId) {
        try {
            return ZoneId.of(zoneId);
        } catch (Exception e) {
            return ZoneId.of("America/Sao_Paulo");
        }
    }

    @Bean
    public DateTimeProvider auditingDateTimeProvider(Clock clock) {
        return () -> Optional.of(Instant.now(clock));
    }
}
