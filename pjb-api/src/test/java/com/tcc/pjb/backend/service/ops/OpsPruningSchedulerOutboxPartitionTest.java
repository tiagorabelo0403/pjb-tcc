package com.tcc.pjb.backend.service.ops;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.YearMonth;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

class OpsPruningSchedulerOutboxPartitionTest {

    private static JdbcTemplate jdbcMock() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.update(contains("delete from tb_outbox_event"), (Object[]) org.mockito.ArgumentMatchers.any())).thenReturn(0);
        when(jdbc.update(contains("delete from tb_idempotency"), (Object[]) org.mockito.ArgumentMatchers.any())).thenReturn(0);
        return jdbc;
    }

    private static Environment envWith(String retentionDays) {
        Environment env = mock(Environment.class);
        when(env.getProperty("pjb.ops.prune.enabled", "true")).thenReturn("true");
        when(env.getProperty("pjb.ops.prune.retentionDays", "7")).thenReturn(retentionDays);
        return env;
    }

    @Test
    void deveDroparParticaoInteiramenteForaDeRetencao() {
        JdbcTemplate jdbc = jdbcMock();
        OpsPruningScheduler scheduler = new OpsPruningScheduler(jdbc, envWith("7"));

        YearMonth muitoAntigo = YearMonth.now().minusMonths(3);
        String partitionName = OutboxPartitionScheduler.partitionName(muitoAntigo);

        scheduler.prune();

        verify(jdbc).execute("DROP TABLE IF EXISTS " + partitionName);
    }

    @Test
    void naoDeveDroparParticaoDoMesCorrente() {
        JdbcTemplate jdbc = jdbcMock();
        OpsPruningScheduler scheduler = new OpsPruningScheduler(jdbc, envWith("7"));

        YearMonth current = YearMonth.now();
        String currentPartition = OutboxPartitionScheduler.partitionName(current);

        scheduler.prune();

        verify(jdbc, never()).execute("DROP TABLE IF EXISTS " + currentPartition);
    }

    @Test
    void naoDeveDroparParticaoDentroDeRetencao() {
        JdbcTemplate jdbc = jdbcMock();
        OpsPruningScheduler scheduler = new OpsPruningScheduler(jdbc, envWith("7"));

        YearMonth previousMonth = YearMonth.now().minusMonths(1);
        String partitionName = OutboxPartitionScheduler.partitionName(previousMonth);

        scheduler.prune();

        verify(jdbc, never()).execute("DROP TABLE IF EXISTS " + partitionName);
    }

    @Test
    void falhaAoDroparParticaoNaoPropagarExcecao() {
        JdbcTemplate jdbc = jdbcMock();
        org.mockito.Mockito.doThrow(new RuntimeException("drop error"))
                .when(jdbc).execute(contains("DROP TABLE IF EXISTS tb_outbox_event_y"));
        OpsPruningScheduler scheduler = new OpsPruningScheduler(jdbc, envWith("7"));

        scheduler.prune();
    }
}
