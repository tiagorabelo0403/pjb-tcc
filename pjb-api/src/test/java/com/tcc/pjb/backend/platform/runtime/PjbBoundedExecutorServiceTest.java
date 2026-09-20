package com.tcc.pjb.backend.platform.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class PjbBoundedExecutorServiceTest {

    @Test
    void shouldBackpressureInsteadOfRejectingWhenLaneDoesNotRejectOnSaturation() throws Exception {
        PjbBoundedExecutorService executor = new PjbBoundedExecutorService("pjb-test-", 1, false, Duration.ofSeconds(5), Duration.ofMillis(20));
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch secondFinished = new CountDownLatch(1);
        CountDownLatch submitterAtExecute = new CountDownLatch(1);
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
            submitterAtExecute.countDown();
            executor.execute(secondFinished::countDown);
        });

        assertThat(submitterAtExecute.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(aguardarBloqueio(submitter))
                .as("submissora precisa ficar bloqueada dentro de execute enquanto a unica faixa esta "
                        + "ocupada; sem isso o teste nao distingue contrapressao de tarefa que passou direto")
                .isTrue();
        assertThat(secondFinished.getCount()).isEqualTo(1L);

        releaseFirst.countDown();
        submitter.join(1500L);
        assertThat(secondFinished.await(1, TimeUnit.SECONDS)).isTrue();
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

    private static boolean aguardarBloqueio(Thread submitter) throws InterruptedException {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (System.nanoTime() < limite) {
            Thread.State estado = submitter.getState();
            if (estado == Thread.State.WAITING || estado == Thread.State.TIMED_WAITING) {
                return true;
            }
            if (estado == Thread.State.TERMINATED) {
                return false;
            }
            Thread.sleep(5L);
        }
        return false;
    }
}
