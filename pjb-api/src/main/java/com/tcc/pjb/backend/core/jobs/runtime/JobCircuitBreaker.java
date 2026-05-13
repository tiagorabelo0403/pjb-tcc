package com.tcc.pjb.backend.core.jobs.runtime;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.core.jobs.domain.JobType;

@Component
@EnableConfigurationProperties(JobCircuitBreakerProperties.class)
public class JobCircuitBreaker {

    private static final Logger log = LoggerFactory.getLogger(JobCircuitBreaker.class);

    private final JobCircuitBreakerProperties props;
    private final JobClaimDao claimDao;

    private final Map<JobType, State> states = new EnumMap<>(JobType.class);

    public JobCircuitBreaker(JobCircuitBreakerProperties props, JobClaimDao claimDao) {
        this.props = Objects.requireNonNull(props);
        this.claimDao = Objects.requireNonNull(claimDao);
        for (JobType t : JobType.values()) {
            states.put(t, new State());
        }
    }

    public void onSuccess(JobType type) {
        State s = states.get(type);
        if (s != null) {
            s.failures.set(0);
        }
    }

    public void onFailure(JobType type, Throwable err) {
        State s = states.get(type);
        if (s == null) return;

        Instant now = Instant.now();
        String reasonToPause = null;
        Instant pausedUntil = null;
        synchronized (s) {
            if (s.windowStart == null || s.windowStart.plusSeconds(props.getWindowSeconds()).isBefore(now)) {
                s.windowStart = now;
                s.failures.set(0);
            }

            int failures = s.failures.incrementAndGet();
            if (s.pausedUntil != null && s.pausedUntil.isAfter(now)) {
                return;
            }

            if (failures >= props.getFailureThreshold()) {
                pausedUntil = now.plusSeconds(props.getPauseSeconds());
                s.pausedUntil = pausedUntil;
                reasonToPause = "circuit-breaker: failures=" + failures + " windowSeconds=" + props.getWindowSeconds();
            }
        }
        if (reasonToPause != null) {
            claimDao.pauseAllByType(type.name(), reasonToPause, now);
            log.warn("jobs circuit-breaker OPEN type={} until={} reason={}", type, pausedUntil, reasonToPause);
        }
    }

    @Scheduled(fixedDelayString = "PT5S")
    public void autoResume() {
        Instant now = Instant.now();
        for (Map.Entry<JobType, State> e : states.entrySet()) {
            State s = e.getValue();
            if (s.pausedUntil == null) continue;
            if (s.pausedUntil.isAfter(now)) continue;
            boolean doResume = false;
            synchronized (s) {
                if (s.pausedUntil != null && !s.pausedUntil.isAfter(now)) {
                    doResume = true;
                    s.pausedUntil = null;
                    s.failures.set(0);
                    s.windowStart = now;
                }
            }
            if (doResume) {
                claimDao.resumeAllByType(e.getKey().name(), now);
                log.info("jobs circuit-breaker CLOSED type={}", e.getKey());
            }
        }
    }

    private static final class State {
        private Instant windowStart;
        private Instant pausedUntil;
        private final AtomicInteger failures = new AtomicInteger(0);
    }
}
