package com.tcc.pjb.backend.service.ops;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.YearMonth;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class OutboxPartitionSchedulerTest {

    @Test
    void partitionNameFormatoCorreto() {
        assertThat(OutboxPartitionScheduler.partitionName(YearMonth.of(2026, 6)))
                .isEqualTo("tb_outbox_event_y2026m06");
        assertThat(OutboxPartitionScheduler.partitionName(YearMonth.of(2027, 12)))
                .isEqualTo("tb_outbox_event_y2027m12");
    }

    @Test
    void createPartitionExecutaSqlComNomeCorreto() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        OutboxPartitionScheduler scheduler = new OutboxPartitionScheduler(jdbc);

        scheduler.createPartition(YearMonth.of(2026, 8));

        verify(jdbc, times(1)).execute(
                "CREATE TABLE IF NOT EXISTS tb_outbox_event_y2026m08 PARTITION OF tb_outbox_event FOR VALUES FROM (202608) TO (202609)"
        );
    }

    @Test
    void createPartitionCriaIndiceDedupNaParticao() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        OutboxPartitionScheduler scheduler = new OutboxPartitionScheduler(jdbc);

        scheduler.createPartition(YearMonth.of(2027, 3));

        verify(jdbc, times(1)).execute(
                "CREATE UNIQUE INDEX IF NOT EXISTS uk_outbox_dedup_y2027m03 ON tb_outbox_event_y2027m03 (dedup_key) WHERE dedup_key IS NOT NULL"
        );
    }

    @Test
    void createPartitionEIdempotente() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        OutboxPartitionScheduler scheduler = new OutboxPartitionScheduler(jdbc);

        scheduler.createPartition(YearMonth.of(2026, 9));
        scheduler.createPartition(YearMonth.of(2026, 9));

        verify(jdbc, times(2)).execute(
                "CREATE TABLE IF NOT EXISTS tb_outbox_event_y2026m09 PARTITION OF tb_outbox_event FOR VALUES FROM (202609) TO (202610)"
        );
    }

    @Test
    void falhaAoCriarParticaoNaoPropagarExcecao() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        org.mockito.Mockito.doThrow(new RuntimeException("db error")).when(jdbc).execute(anyString());
        OutboxPartitionScheduler scheduler = new OutboxPartitionScheduler(jdbc);

        scheduler.createPartition(YearMonth.of(2026, 10));
    }
}
