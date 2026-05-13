package com.tcc.pjb.backend.core.jobs.runtime;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@EnableConfigurationProperties(JobHousekeepingProperties.class)
public class JobHousekeepingScheduler {

    private static final Logger log = LoggerFactory.getLogger(JobHousekeepingScheduler.class);

    private final JdbcTemplate jdbc;
    private final JobHousekeepingProperties props;

    public JobHousekeepingScheduler(JdbcTemplate jdbc, JobHousekeepingProperties props) {
        this.jdbc = Objects.requireNonNull(jdbc);
        this.props = Objects.requireNonNull(props);
    }

    @Scheduled(fixedDelayString = "PT6H")
    @Transactional
    public void run() {
        Instant now = Instant.now();
        Instant stale = now.minusSeconds(Math.max(30, props.getStaleLockSeconds()));

        int unlocked = jdbc.update(
                "UPDATE tb_job SET status='FAILED', next_retry_at=?, locked_by=NULL, locked_at=NULL, updated_at=?, last_error=COALESCE(last_error,'') || '\n[housekeeping] stale lock unlocked' " +
                        "WHERE status='RUNNING' AND locked_at IS NOT NULL AND locked_at < ?",
                now, now, stale
        );

        int forcedDead = jdbc.update(
                "UPDATE tb_job SET status='DEAD', next_retry_at=NULL, updated_at=? WHERE status IN ('FAILED','RUNNING') AND attempts >= max_attempts",
                now
        );

        int deleted = 0;
        int days = Math.max(1, props.getDeleteSucceededAfterDays());
        Instant cutoff = now.minus(days, ChronoUnit.DAYS);
        deleted += jdbc.update("DELETE FROM tb_job WHERE status='SUCCEEDED' AND updated_at < ?", cutoff);

        if (unlocked + forcedDead + deleted > 0) {
            log.info("jobs housekeeping: unlocked={} forcedDead={} deleted={} cutoffDays={}", unlocked, forcedDead, deleted, days);
        }
    }
}
