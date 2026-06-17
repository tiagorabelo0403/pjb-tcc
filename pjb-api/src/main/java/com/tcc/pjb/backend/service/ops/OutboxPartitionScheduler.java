package com.tcc.pjb.backend.service.ops;

import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxPartitionScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxPartitionScheduler.class);

    private final JdbcTemplate jdbc;

    public OutboxPartitionScheduler(JdbcTemplate jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc);
    }

    @Scheduled(cron = "${pjb.ops.outbox-partition.cron:0 0 3 25 * *}")
    public void createNextMonthPartition() {
        YearMonth nextMonth = YearMonth.now(ZoneOffset.UTC).plusMonths(1);
        createPartition(nextMonth);
    }

    void createPartition(YearMonth month) {
        String name = partitionName(month);
        int fromVal = month.getYear() * 100 + month.getMonthValue();
        YearMonth next = month.plusMonths(1);
        int toVal = next.getYear() * 100 + next.getMonthValue();
        String sql = String.format(
                "CREATE TABLE IF NOT EXISTS %s PARTITION OF tb_outbox_event FOR VALUES FROM (%d) TO (%d)",
                name, fromVal, toVal
        );
        try {
            jdbc.execute(sql);
            createDedupIndex(name);
            log.info("[OUTBOX-PARTITION] partition created: {} [{}, {})", name, fromVal, toVal);
        } catch (Exception e) {
            log.warn("[OUTBOX-PARTITION] failed to create partition {}: {}", name, e.getMessage());
        }
    }

    private void createDedupIndex(String partitionName) {
        String idxSuffix = partitionName.replace("tb_outbox_event_", "");
        try {
            jdbc.execute(String.format(
                    "CREATE UNIQUE INDEX IF NOT EXISTS uk_outbox_dedup_%s ON %s (dedup_key) WHERE dedup_key IS NOT NULL",
                    idxSuffix, partitionName
            ));
        } catch (Exception e) {
            log.warn("[OUTBOX-PARTITION] failed to create dedup index on {}: {}", partitionName, e.getMessage());
        }
    }

    static String partitionName(YearMonth ym) {
        return String.format("tb_outbox_event_y%04dm%02d", ym.getYear(), ym.getMonthValue());
    }
}
