package com.tcc.pjb.backend.core.jobs.runtime;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.jobs.domain.JobState;
import com.tcc.pjb.backend.core.jobs.domain.JobStates;
import com.tcc.pjb.backend.core.jobs.persistence.entity.Job;
import com.tcc.pjb.backend.core.jobs.persistence.repo.JobItemRepository;
import com.tcc.pjb.backend.core.jobs.persistence.repo.JobRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;









@Service
public class JobExecutionService {

  private static final Logger log = LoggerFactory.getLogger(JobExecutionService.class);

  private final JobRepository jobRepository;
  private final JobItemRepository jobItemRepository;
  private final ObjectMapper mapper;
  private final JobClaimDao claimDao;
  private final Map<com.tcc.pjb.backend.core.jobs.domain.JobType, JobHandler> handlers;
  private final JobCircuitBreaker circuitBreaker;
  private final Counter jobsSucceeded;
  private final Counter jobsFailed;
  private final Counter jobsDead;
  private final TransactionTemplate tx;

  public JobExecutionService(JobRepository jobRepository,
                             JobItemRepository jobItemRepository,
                             ObjectMapper mapper,
                             JobClaimDao claimDao,
                             List<JobHandler> handlers,
                             JobCircuitBreaker circuitBreaker,
                             MeterRegistry registry,
                             PlatformTransactionManager txManager) {
    this.jobRepository = Objects.requireNonNull(jobRepository);
    this.jobItemRepository = Objects.requireNonNull(jobItemRepository);
    this.mapper = Objects.requireNonNull(mapper);
    this.claimDao = Objects.requireNonNull(claimDao);
    this.circuitBreaker = Objects.requireNonNull(circuitBreaker);

    Map<com.tcc.pjb.backend.core.jobs.domain.JobType, JobHandler> m = new EnumMap<>(com.tcc.pjb.backend.core.jobs.domain.JobType.class);
    for (JobHandler h : Objects.requireNonNull(handlers)) {
      m.put(h.type(), h);
    }
    this.handlers = Map.copyOf(m);

    this.jobsSucceeded = registry.counter("jobs.succeeded");
    this.jobsFailed = registry.counter("jobs.failed");
    this.jobsDead = registry.counter("jobs.dead");
    this.tx = new TransactionTemplate(Objects.requireNonNull(txManager));
  }

  public void execute(UUID jobId, String instanceId) {
    Objects.requireNonNull(jobId, "jobId");
    Objects.requireNonNull(instanceId, "instanceId");

    Job job = tx.execute(status -> jobRepository.findById(jobId).orElse(null));
    if (job == null) {
      return;
    }

    JobHandler handler = handlers.get(job.getType());
    if (handler == null) {
      
      try {
        job.fail("Sem handler registrado para type=" + job.getType(), null);
        jobRepository.save(job);
      } catch (Exception e) {
        log.warn("Falha ao marcar job sem handler: jobId={} err={}", jobId, e.getMessage());
      }
      jobsDead.increment();
      return;
    }

    Outcome outcome;
    try {
      JobExecutionContext ctx = new JobExecutionContext(instanceId, job, mapper, jobItemRepository, claimDao);
      handler.execute(ctx);
      outcome = Outcome.succeeded();
    } catch (JobPauseException p) {
      outcome = Outcome.paused(p.reason());
    } catch (Exception e) {
      String err = safeStack(e, 6000);
      Instant next = nextRetryAt(job.getAttempts(), Duration.ofSeconds(2), Duration.ofMinutes(5));
      outcome = Outcome.failed(err, next, e);
    }

    final Outcome finalOutcome = outcome;

    Job persisted = tx.execute(status -> {
      Job j = jobRepository.findById(jobId).orElse(null);
      if (j == null) {
        return null;
      }
      finalOutcome.apply(j);
      return jobRepository.save(j);
    });

    if (persisted != null && persisted.getStatus() != null) {
      JobState state = JobStates.from(persisted.getStatus());
      afterPersist(state, persisted, outcome);
    }
  }

  private sealed interface Outcome permits OutcomeSucceeded, OutcomePaused, OutcomeFailed {
    void apply(Job j);

    static Outcome succeeded() {
      return new OutcomeSucceeded();
    }

    static Outcome paused(String reason) {
      return new OutcomePaused(reason);
    }

    static Outcome failed(String err, Instant next, Exception ex) {
      return new OutcomeFailed(err, next, ex);
    }
  }

  private static final class OutcomeSucceeded implements Outcome {
    @Override
    public void apply(Job j) {
      j.succeed();
    }
  }

  private record OutcomePaused(String reason) implements Outcome {
    @Override
    public void apply(Job j) {
      j.pause(reason);
    }
  }

  private record OutcomeFailed(String err, Instant next, Exception ex) implements Outcome {
    @Override
    public void apply(Job j) {
      j.fail(err, next);
    }
  }

  private void afterPersist(JobState state, Job persisted, Outcome outcome) {
    switch (state) {
      case JobState.Succeeded s -> {
        jobsSucceeded.increment();
        circuitBreaker.onSuccess(persisted.getType());
      }
      case JobState.Paused paused -> {
        
      }
      case JobState.Failed f -> {
        jobsFailed.increment();
        if (outcome instanceof OutcomeFailed of) {
          circuitBreaker.onFailure(persisted.getType(), of.ex);
          log.warn("job falhou: id={} type={} attempts={}/{} status={} err={}",
              persisted.getId(), persisted.getType(), persisted.getAttempts(), persisted.getMaxAttempts(), persisted.getStatus(),
              of.ex != null ? of.ex.toString() : "");
        } else {
          circuitBreaker.onFailure(persisted.getType(), new RuntimeException("job_failed"));
        }
      }
      case JobState.Dead d -> {
        jobsDead.increment();
        if (outcome instanceof OutcomeFailed of) {
          circuitBreaker.onFailure(persisted.getType(), of.ex);
          log.warn("job dead: id={} type={} attempts={}/{} status={} err={}",
              persisted.getId(), persisted.getType(), persisted.getAttempts(), persisted.getMaxAttempts(), persisted.getStatus(),
              of.ex != null ? of.ex.toString() : "");
        } else {
          circuitBreaker.onFailure(persisted.getType(), new RuntimeException("job_dead"));
        }
      }
      case JobState.Running r -> {
        
      }
      case JobState.Pending pending -> {
        
      }
    }
  }

  private static Instant nextRetryAt(int attemptsBeforeIncrement, Duration base, Duration max) {
    int a = Math.max(0, attemptsBeforeIncrement + 1);
    long pow = 1L << Math.min(12, a);
    long backoffMs = Math.min(max.toMillis(), base.toMillis() * pow);
    long jitter = (long) (Math.random() * Math.min(1500, Math.max(1L, backoffMs / 5)));
    return Instant.now().plusMillis(backoffMs + jitter);
  }

  private static String safeStack(Throwable t, int maxChars) {
    if (t == null) return "";
    StringWriter sw = new StringWriter();
    t.printStackTrace(new PrintWriter(sw));
    String s = sw.toString();
    if (s.length() <= maxChars) {
      return s;
    }
    return s.substring(0, maxChars);
  }
}
