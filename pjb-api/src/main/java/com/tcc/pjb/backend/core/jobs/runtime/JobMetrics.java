package com.tcc.pjb.backend.core.jobs.runtime;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class JobMetrics {

    private final JdbcTemplate jdbc;
    private final AtomicLong running = new AtomicLong(0);
    private final AtomicLong dead = new AtomicLong(0);

    public JobMetrics(JdbcTemplate jdbc, MeterRegistry registry) {
        this.jdbc = Objects.requireNonNull(jdbc);
        Gauge.builder("jobs.running", running, AtomicLong::get).register(registry);
        Gauge.builder("jobs.dead.current", dead, AtomicLong::get).register(registry);
    }

    @Scheduled(fixedDelayString = "PT30S")
    public void refresh() {
        Long r = jdbc.queryForObject("SELECT COUNT(*) FROM tb_job WHERE status='RUNNING'", Long.class);
        Long d = jdbc.queryForObject("SELECT COUNT(*) FROM tb_job WHERE status='DEAD'", Long.class);
        running.set(r != null ? r : 0L);
        dead.set(d != null ? d : 0L);
    }
}
