package com.tcc.pjb.backend.core.jobs.runtime;

import com.tcc.pjb.backend.platform.concurrent.PjbVirtualThreadSpine;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import com.tcc.pjb.backend.service.infra.scaling.JudicialScaleProfileResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@ConditionalOnProperty(prefix = "pjb.jobs.dispatcher", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(JobDispatcherProperties.class)
public class JobDispatcherLoop implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(JobDispatcherLoop.class);

    private final JobDispatcherProperties props;
    private final JobClaimDao claimDao;
    private final JobExecutionService executionService;
    private final JobNotifySignal notifySignal;
    private final JobInstanceIdProvider instanceIdProvider;
    private final JobPermitLeaser permitLeaser;
    private final TransactionTemplate tx;
    private final JudicialScaleProfileResolver judicialScaleProfileResolver;
    private final ExecutorService batchExecutor;
    private final DelayQueue<JobTimeout> timeouts = new DelayQueue<>();

    private static final class JobTimeout implements Delayed {
        private final Future<?> task;
        private final long deadlineNanos;

        private JobTimeout(Future<?> task, long budgetMs) {
            this.task = task;
            this.deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(Math.max(1L, budgetMs));
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(deadlineNanos - System.nanoTime(), TimeUnit.NANOSECONDS);
        }

        @Override
        public int compareTo(Delayed other) {
            if (other instanceof JobTimeout timeout) {
                return Long.compare(deadlineNanos, timeout.deadlineNanos);
            }
            return Long.compare(getDelay(TimeUnit.NANOSECONDS), other.getDelay(TimeUnit.NANOSECONDS));
        }
    }

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicReference<Thread> loopThread = new AtomicReference<>();
    private final AtomicReference<Thread> timeoutWatcherThread = new AtomicReference<>();
    private final ReentrantLock lifecycleLock = new ReentrantLock();

    public JobDispatcherLoop(JobDispatcherProperties props,
                             JobClaimDao claimDao,
                             JobExecutionService executionService,
                             JobNotifySignal notifySignal,
                             JobInstanceIdProvider instanceIdProvider,
                             JobPermitLeaser permitLeaser,
                             JudicialScaleProfileResolver judicialScaleProfileResolver,
                             PlatformTransactionManager txm,
                             @Qualifier("jobVirtualThreadExecutor") ExecutorService jobVirtualThreadExecutor) {
        this.props = Objects.requireNonNull(props);
        this.claimDao = Objects.requireNonNull(claimDao);
        this.executionService = Objects.requireNonNull(executionService);
        this.notifySignal = Objects.requireNonNull(notifySignal);
        this.instanceIdProvider = Objects.requireNonNull(instanceIdProvider);
        this.permitLeaser = Objects.requireNonNull(permitLeaser);
        this.judicialScaleProfileResolver = Objects.requireNonNull(judicialScaleProfileResolver);
        this.tx = new TransactionTemplate(Objects.requireNonNull(txm));
        this.batchExecutor = Objects.requireNonNull(jobVirtualThreadExecutor);
    }

    @Override
    public void start() {
        if (!props.isEnabled()) {
            log.info("JobDispatcher desabilitado por configuração");
            return;
        }
        lifecycleLock.lock();
        try {
            if (!running.compareAndSet(false, true)) {
                return;
            }
            loopThread.set(PjbVirtualThreadSpine.start("jobs-dispatcher", this::loop));
            timeoutWatcherThread.set(PjbVirtualThreadSpine.start("jobs-timeout-watch", this::watchTimeouts));
            log.info("JobDispatcher iniciado instanceId={}", instanceIdProvider.get());
        } finally {
            lifecycleLock.unlock();
        }
    }

    private void watchTimeouts() {
        while (running.get()) {
            try {
                JobTimeout timeout = timeouts.poll(250, TimeUnit.MILLISECONDS);
                if (timeout == null) {
                    continue;
                }
                if (!timeout.task.isDone()) {
                    timeout.task.cancel(true);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                if (!running.get()) {
                    return;
                }
            } catch (Exception ex) {
                log.debug("Falha no watcher de timeout de jobs: {}", ex.getMessage());
            }
        }
    }

    private void loop() {
        String instanceId = instanceIdProvider.get();
        int batchSize = Math.max(1, props.getClaimBatchSize());
        long ttlSec = Math.max(10, props.getLockTtlSeconds());
        long idleBackoff = props.getBackoffMinMillis();
        while (running.get()) {
            try {
                Instant now = Instant.now();
                Instant expiredAt = now.minusSeconds(ttlSec);
                List<JobClaim> claimed = tx.execute(status -> claimDao.claimNext(instanceId, now, expiredAt, batchSize));
                if (claimed == null || claimed.isEmpty()) {
                    boolean woke = notifySignal.await(idleBackoff);
                    if (woke) {
                        notifySignal.drain();
                        idleBackoff = props.getBackoffMinMillis();
                    } else {
                        idleBackoff = Math.min(props.getBackoffMaxMillis(), idleBackoff + props.getBackoffStepMillis());
                    }
                    continue;
                }
                idleBackoff = props.getBackoffMinMillis();
                Instant deadline = Instant.now().plus(Math.max(50, props.getBatchJoinMillis()), ChronoUnit.MILLIS);
                List<Future<?>> batchTasks = new ArrayList<>(claimed.size());
                for (JobClaim claim : claimed) {
                    JudicialScaleProfileResolver.JudicialScalePolicy scalePolicy = judicialScaleProfileResolver.resolvePolicyFromInbox(claim.inboxKey(), claim.type());
                    long budgetMs = props.budgetMillisForType(claim.type(), scalePolicy == null ? 1d : scalePolicy.queueBudgetFactor());
                    Future<?> task = batchExecutor.submit(() -> executeClaim(instanceId, claim));
                    batchTasks.add(task);
                    timeouts.offer(new JobTimeout(task, budgetMs));
                }
                awaitBatch(batchTasks, deadline);
                if (claimed.size() < batchSize) {
                    sleep(25);
                }
            } catch (Exception ex) {
                log.error("dispatcher loop erro: {}", ex.getMessage(), ex);
                sleep(1000);
            }
        }
    }

    private void executeClaim(String instanceId, JobClaim claim) {
        boolean ok = false;
        try (JobPermitLeaser.Lease lease = permitLeaser.acquire(new JobPermitLeaser.Request(claim.type(), claim.ownerUserId(), claim.uf(), claim.orgao(), claim.inboxKey()))) {
            tx.executeWithoutResult(status -> executionService.execute(claim.id(), instanceId));
            ok = true;
        } catch (Throwable ex) {
            log.debug("Falha na execução do job {}: {}", claim.id(), ex.getMessage());
        } finally {
            if (ok) {
                permitLeaser.recordSuccess(claim.type());
            } else {
                permitLeaser.recordFailure(claim.type());
            }
        }
    }

    private void awaitBatch(List<Future<?>> tasks, Instant deadline) {
        for (Future<?> task : tasks) {
            long remainingMs = Math.max(1L, Duration.between(Instant.now(), deadline).toMillis());
            try {
                task.get(remainingMs, TimeUnit.MILLISECONDS);
            } catch (java.util.concurrent.TimeoutException ex) {
                task.cancel(true);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                task.cancel(true);
                return;
            } catch (Exception ex) {
                log.debug("Falha durante aguardando batch de jobs: {}", ex.getMessage());
            }
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(Math.max(0, ms));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void stop() {
        lifecycleLock.lock();
        try {
            running.set(false);
            Thread loop = loopThread.getAndSet(null);
            if (loop != null) {
                loop.interrupt();
            }
            Thread timeoutWatcher = timeoutWatcherThread.getAndSet(null);
            if (timeoutWatcher != null) {
                timeoutWatcher.interrupt();
            }
        } finally {
            lifecycleLock.unlock();
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isAutoStartup() {
        return props.isEnabled();
    }
}
