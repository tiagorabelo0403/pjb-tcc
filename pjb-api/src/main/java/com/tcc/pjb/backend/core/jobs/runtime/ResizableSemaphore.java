package com.tcc.pjb.backend.core.jobs.runtime;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public final class ResizableSemaphore extends Semaphore {

    private final AtomicInteger capacity;

    public ResizableSemaphore(int permits) {
        super(permits);
        this.capacity = new AtomicInteger(Math.max(0, permits));
    }

    public int capacity() {
        return capacity.get();
    }

    public int inUse() {
        return Math.max(0, capacity() - availablePermits());
    }

    public boolean isIdle() {
        return availablePermits() >= capacity() && !hasQueuedThreads();
    }

    public void expand(int delta) {
        if (delta <= 0) {
            return;
        }
        capacity.addAndGet(delta);
        super.release(delta);
    }

    public void reduce(int reduction) {
        if (reduction <= 0) {
            return;
        }
        int boundedReduction = Math.min(reduction, Math.max(0, capacity.get()));
        if (boundedReduction <= 0) {
            return;
        }
        capacity.addAndGet(-boundedReduction);
        super.reducePermits(boundedReduction);
    }
}
