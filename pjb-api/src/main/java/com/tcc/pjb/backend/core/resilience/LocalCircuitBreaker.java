package com.tcc.pjb.backend.core.resilience;

import java.time.Clock;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class LocalCircuitBreaker {

  public enum State {
    CLOSED,
    OPEN,
    HALF_OPEN
  }

  private final Clock clock;
  private final int failureThreshold;
  private final long openMillis;
  private final AtomicInteger failures = new AtomicInteger();
  private final AtomicLong openedAt = new AtomicLong();
  private volatile State state = State.CLOSED;

  public LocalCircuitBreaker(Clock clock, int failureThreshold, long openMillis) {
    this.clock = Objects.requireNonNull(clock);
    this.failureThreshold = Math.max(1, failureThreshold);
    this.openMillis = Math.max(1000L, openMillis);
  }

  public boolean tryAcquire() {
    long now = clock.millis();
    State s = state;
    if (s == State.CLOSED) return true;
    if (s == State.OPEN) {
      long t = openedAt.get();
      if (now - t >= openMillis) {
        state = State.HALF_OPEN;
        return true;
      }
      return false;
    }
    return true;
  }

  public void recordSuccess() {
    failures.set(0);
    state = State.CLOSED;
    openedAt.set(0);
  }

  public void recordFailure() {
    int f = failures.incrementAndGet();
    if (f >= failureThreshold) {
      state = State.OPEN;
      openedAt.set(clock.millis());
    }
  }

  public State state() {
    return state;
  }
}
