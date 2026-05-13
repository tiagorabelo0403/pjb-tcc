package com.tcc.pjb.backend.core.jobs.runtime;

import com.tcc.pjb.backend.platform.concurrent.PjbVirtualThreadSpine;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(name = "pgListenDataSource")
@ConditionalOnProperty(prefix = "pjb.jobs.pg-listen", name = "enabled", havingValue = "true", matchIfMissing = true)
public final class PgJobListenNotifyService implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(PgJobListenNotifyService.class);

    private final DataSource listenDataSource;
    private final JobNotifySignal signal;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicReference<Thread> listenerThread = new AtomicReference<>();
    private final ReentrantLock lifecycleLock = new ReentrantLock();

    public PgJobListenNotifyService(@Qualifier("pgListenDataSource") DataSource listenDataSource, JobNotifySignal signal) {
        this.listenDataSource = Objects.requireNonNull(listenDataSource);
        this.signal = Objects.requireNonNull(signal);
    }

    @Override
    public void start() {
        lifecycleLock.lock();
        try {
            if (!running.compareAndSet(false, true)) {
                return;
            }
            listenerThread.set(PjbVirtualThreadSpine.start("pg-listen-pjb-job", this::loop));
        } finally {
            lifecycleLock.unlock();
        }
    }

    private void loop() {
        while (running.get()) {
            try (Connection c = listenDataSource.getConnection()) {
                c.setAutoCommit(true);
                try (Statement st = c.createStatement()) {
                    st.execute("LISTEN pjb_job");
                }
                log.info("PG LISTEN ativo canal=pjb_job");
                try {
                    while (running.get()) {
                        Object pg = unwrapPgConnection(c);
                        if (pg != null) {
                            int n = drainNotifications(pg, 20_000);
                            if (n > 0) {
                                signal.wake();
                            }
                            continue;
                        }
                        Thread.sleep(Duration.ofSeconds(10));
                        signal.wake();
                    }
                } finally {
                    try (Statement st = c.createStatement()) {
                        st.execute("UNLISTEN *");
                    } catch (Exception e) {
                        log.debug("PG UNLISTEN falhou", e);
                    }
                }
            } catch (Exception e) {
                log.warn("PG LISTEN falhou, retomando: {}", e.toString());
                sleep(1500);
            }
        }
    }

    private static Object unwrapPgConnection(Connection c) {
        try {
            Class<?> pg = Class.forName("org.postgresql.PGConnection");
            if (pg.isInstance(c)) return c;
            return c.unwrap(pg);
        } catch (Exception e) {
            return null;
        }
    }

    private static int drainNotifications(Object pgConn, int timeoutMs) {
        try {
            var m = pgConn.getClass().getMethod("getNotifications", int.class);
            Object arr = m.invoke(pgConn, timeoutMs);
            if (arr == null) return 0;
            Object[] a = (Object[]) arr;
            return a.length;
        } catch (NoSuchMethodException e) {
            try {
                var m = pgConn.getClass().getMethod("getNotifications");
                Object arr = m.invoke(pgConn);
                if (arr == null) return 0;
                Object[] a = (Object[]) arr;
                return a.length;
            } catch (Exception ex) {
                return 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void stop() {
        lifecycleLock.lock();
        try {
            running.set(false);
            Thread thread = listenerThread.getAndSet(null);
            if (thread != null) {
                thread.interrupt();
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
        return true;
    }
}
