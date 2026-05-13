package com.tcc.pjb.backend.platform.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class PjbBoundedExecutorServiceTest {

    @Test
    void shouldBackpressureInsteadOfRejectingWhenLaneDoesNotRejectOnSaturation() throws Exception {
        PjbBoundedExecutorService executor = new PjbBoundedExecutorService("pjb-test-", 1, false, Duration.ofSeconds(5), Duration.ofMillis(20));
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch secondFinished = new CountDownLatch(1);
        AtomicLong waitedMillis = new AtomicLong();
        executor.execute(() -> {
            firstStarted.countDown();
            try {
                releaseFirst.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        });
        assertThat(firstStarted.await(1, TimeUnit.SECONDS)).isTrue();
        Thread submitter = Thread.ofPlatform().start(() -> {
            long startedAt = System.nanoTime();
            executor.execute(() -> {
                waitedMillis.set(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt));
                secondFinished.countDown();
            });
        });
        Thread.sleep(100L);
        assertThat(secondFinished.getCount()).isEqualTo(1L);
        releaseFirst.countDown();
        submitter.join(1500L);
        assertThat(secondFinished.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(waitedMillis.get()).isGreaterThanOrEqualTo(50L);
        executor.close();
    }

    @Test
    void shouldRejectNewTasksAfterDrainBegins() {
        PjbBoundedExecutorService executor = new PjbBoundedExecutorService("pjb-test-", 1, true, Duration.ofSeconds(5), Duration.ofMillis(20));
        executor.beginDrain("shutdown");
        assertThat(executor.acceptingTasks()).isFalse();
        assertThatThrownBy(() -> executor.execute(() -> {
        })).isInstanceOf(RejectedExecutionException.class);
        executor.close();
    }
}
