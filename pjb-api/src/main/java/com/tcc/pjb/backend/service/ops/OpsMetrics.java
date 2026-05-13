package com.tcc.pjb.backend.service.ops;

import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class OpsMetrics {

  private final JdbcTemplate jdbc;

  public OpsMetrics(MeterRegistry registry, JdbcTemplate jdbc) {
    this.jdbc = Objects.requireNonNull(jdbc);

    Gauge.builder("outbox.pending", this, OpsMetrics::outboxPending)
        .description("Quantidade de eventos pendentes na outbox")
        .register(registry);

    Gauge.builder("secretariat.queue.size", this, OpsMetrics::secretariatQueueSize)
        .description("Tamanho da fila operacional da secretaria")
        .register(registry);

    Gauge.builder("upload.inflight", this, OpsMetrics::uploadInflight)
        .description("Uploads em andamento")
        .register(registry);

    Gauge.builder("triage.lag.ms", this, OpsMetrics::triageLagMs)
        .description("Latência (ms) do item mais antigo pendente na fila")
        .register(registry);
  }

  private double outboxPending() {
    return (double) safeCount("select count(1) from tb_outbox_event where status = 'PENDING'");
  }

  private double secretariatQueueSize() {
    
    return (double) safeCount("select count(1) from tb_secretariat_queue_item where status not in ('CONCLUIDO','CANCELADO')");
  }

  private double uploadInflight() {
    return (double) safeCount("select count(1) from tb_upload_batch where status = 'INITIATED'");
  }

  private double triageLagMs() {
    
    try {
      Double v = jdbc.queryForObject(
          "select coalesce(extract(epoch from (now() - min(created_at))) * 1000.0, 0.0) from tb_secretariat_queue_item where status = 'PENDENTE'",
          Double.class
      );
      return v == null ? 0.0 : Math.max(0.0, v);
    } catch (Exception ignore) {
      return 0.0;
    }
  }

  private long safeCount(String sql) {
    try {
      Long v = jdbc.queryForObject(sql, Long.class);
      return v == null ? 0L : Math.max(0L, v);
    } catch (Exception ignore) {
      return 0L;
    }
  }
}
