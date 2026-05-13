package com.tcc.pjb.backend.core.jobs.runtime;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public final class JobNotifySignal {

    private final Semaphore sem = new Semaphore(0);

    public void wake() {
        sem.release();
    }

    public boolean await(long timeoutMillis) {
        try {
            return sem.tryAcquire(Math.max(1L, timeoutMillis), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public void drain() {
        sem.drainPermits();
    }
}
