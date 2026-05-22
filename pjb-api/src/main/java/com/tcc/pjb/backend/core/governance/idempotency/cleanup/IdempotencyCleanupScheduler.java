package com.tcc.pjb.backend.core.governance.idempotency.cleanup;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "pjb.governance.idempotency.cleanup", name = "enabled", havingValue = "true", matchIfMissing = true)
public class IdempotencyCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyCleanupScheduler.class);

    private final JdbcTemplate jdbc;
    private final IdempotencyCleanupProperties props;

    public IdempotencyCleanupScheduler(JdbcTemplate jdbc, IdempotencyCleanupProperties props) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
        this.props = Objects.requireNonNull(props, "props");
    }

    @Scheduled(cron = "${pjb.governance.idempotency.cleanup.cron:0 17 3 * * *}")
    public void run() {
        if (!props.isEnabled()) return;

        long lockKey = props.getAdvisoryLockKey();
        Boolean locked = jdbc.queryForObject("select pg_try_advisory_lock(?)", Boolean.class, lockKey);
        if (locked == null || !locked) {
            return;
        }

        Instant threshold = Instant.now().minus(props.getTtl());

        int total = 0;
        try {
            for (int i = 0; i < props.getMaxBatchesPerRun(); i++) {
                int deleted = deleteBatch(threshold, props.getBatchSize());
                total += deleted;
                if (deleted < props.getBatchSize()) {
                    break;
                }
            }
        } catch (Exception e) {
            log.warn("Idempotency cleanup falhou: {}", e.getMessage());
        } finally {
            try {
                jdbc.queryForObject("select pg_advisory_unlock(?)", Boolean.class, lockKey);
            } catch (Exception ignored) {
            }
        }

        if (total > 0) {
            log.info("Idempotency cleanup: removed={} threshold={}", total, threshold);
        }
    }

    private int deleteBatch(Instant threshold, int batchSize) {
        String sql = "with cte as (" +
                "  select request_hash from tb_request_idempotency" +
                "  where updated_at < ?" +
                "  order by updated_at asc" +
                "  limit ?" +
                ") " +
                "delete from tb_request_idempotency t using cte where t.request_hash = cte.request_hash";

        return jdbc.update(sql, Timestamp.from(threshold), batchSize);
    }
}
