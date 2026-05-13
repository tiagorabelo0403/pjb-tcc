package com.tcc.pjb.backend.platform.runtime.execution;

import com.tcc.pjb.backend.configs.datasource.PjbProcessoSigiloRlsContext;
import com.tcc.pjb.backend.core.lgpd.ProcessoSigiloRlsEnvelope;
import com.tcc.pjb.backend.platform.runtime.PjbBoundedExecutorProvider;
import com.tcc.pjb.backend.platform.runtime.PjbBoundedExecutorService;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PjbExecutionOrchestratorTest {

    @Test
    void deveMaterializarSnapshotDaOperacao() throws Exception {
        PjbBoundedExecutorService io = new PjbBoundedExecutorService("test-io-", 4, true, Duration.ofSeconds(5), Duration.ofMillis(100));
        PjbBoundedExecutorService burst = new PjbBoundedExecutorService("test-burst-", 4, true, Duration.ofSeconds(5), Duration.ofMillis(100));
        PjbBoundedExecutorService externalIo = new PjbBoundedExecutorService("test-ext-", 4, true, Duration.ofSeconds(5), Duration.ofMillis(100));
        PjbBoundedExecutorService live = new PjbBoundedExecutorService("test-live-", 4, true, Duration.ofSeconds(5), Duration.ofMillis(100));
        PjbBoundedExecutorService job = new PjbBoundedExecutorService("test-job-", 4, true, Duration.ofSeconds(5), Duration.ofMillis(100));
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        try {
            PjbExecutionOrchestrator orchestrator = new PjbExecutionOrchestrator(new PjbBoundedExecutorProvider(io, burst, externalIo, live, job), scheduler, new PjbProcessoSigiloRlsContext());
            String result = orchestrator.supply(PjbExecutionDescriptor.io("snapshot-op", Duration.ofSeconds(1)), () -> "OK").get(2, TimeUnit.SECONDS);
            assertEquals("OK", result);
            var snapshot = orchestrator.snapshot();
            assertTrue(snapshot.operations().stream().anyMatch(operation -> operation.operationName().equals("snapshot-op") && operation.completedTasks() == 1L));
            assertTrue(snapshot.lanes().stream().anyMatch(lane -> lane.lane().equals("io") && lane.submittedTasks() >= 1L));
        } finally {
            scheduler.shutdownNow();
            io.close();
            burst.close();
            externalIo.close();
            live.close();
            job.close();
        }
    }

    @Test
    void deveMarcarTimeoutQuandoOperacaoExcederBudget() {
        PjbBoundedExecutorService io = new PjbBoundedExecutorService("test-io-", 1, true, Duration.ofSeconds(5), Duration.ofMillis(50));
        PjbBoundedExecutorService burst = new PjbBoundedExecutorService("test-burst-", 1, true, Duration.ofSeconds(5), Duration.ofMillis(50));
        PjbBoundedExecutorService externalIo = new PjbBoundedExecutorService("test-ext-", 1, true, Duration.ofSeconds(5), Duration.ofMillis(50));
        PjbBoundedExecutorService live = new PjbBoundedExecutorService("test-live-", 1, true, Duration.ofSeconds(5), Duration.ofMillis(50));
        PjbBoundedExecutorService job = new PjbBoundedExecutorService("test-job-", 1, true, Duration.ofSeconds(5), Duration.ofMillis(50));
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        try {
            PjbExecutionOrchestrator orchestrator = new PjbExecutionOrchestrator(new PjbBoundedExecutorProvider(io, burst, externalIo, live, job), scheduler, new PjbProcessoSigiloRlsContext());
            ExecutionException ex = assertThrows(ExecutionException.class, () -> orchestrator
                    .supply(PjbExecutionDescriptor.io("timeout-op", Duration.ofMillis(50)), () -> {
                        try {
                            Thread.sleep(200L);
                        } catch (InterruptedException interruptedException) {
                            Thread.currentThread().interrupt();
                        }
                        return "LATE";
                    }).get(1, TimeUnit.SECONDS));
            assertTrue(ex.getCause() instanceof PjbExecutionTimedOutException);
            var snapshot = orchestrator.snapshot();
            assertTrue(snapshot.operations().stream().anyMatch(operation -> operation.operationName().equals("timeout-op") && operation.timedOutTasks() >= 1L));
        } finally {
            scheduler.shutdownNow();
            io.close();
            burst.close();
            externalIo.close();
            live.close();
            job.close();
        }
    }


    @Test
    void deveNaoExecutarFornecedorQuandoFutureExpiraAntesDoAgendamento() throws Exception {
        PjbBoundedExecutorService io = new PjbBoundedExecutorService("test-io-", 1, true, Duration.ofSeconds(5), Duration.ofMillis(50));
        PjbBoundedExecutorService burst = new PjbBoundedExecutorService("test-burst-", 1, true, Duration.ofSeconds(5), Duration.ofMillis(50));
        PjbBoundedExecutorService externalIo = new PjbBoundedExecutorService("test-ext-", 1, true, Duration.ofSeconds(5), Duration.ofMillis(50));
        PjbBoundedExecutorService live = new PjbBoundedExecutorService("test-live-", 1, true, Duration.ofSeconds(5), Duration.ofMillis(50));
        PjbBoundedExecutorService job = new PjbBoundedExecutorService("test-job-", 1, true, Duration.ofSeconds(5), Duration.ofMillis(50));
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        try {
            PjbExecutionOrchestrator orchestrator = new PjbExecutionOrchestrator(new PjbBoundedExecutorProvider(io, burst, externalIo, live, job), scheduler, new PjbProcessoSigiloRlsContext());
            java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.atomic.AtomicBoolean executed = new java.util.concurrent.atomic.AtomicBoolean(false);
            CompletableFuture<String> blocking = orchestrator.supply(PjbExecutionDescriptor.io("blocking-op", Duration.ofSeconds(1)), () -> {
                try {
                    latch.await(1, TimeUnit.SECONDS);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                }
                return "BLOCKED";
            });
            CompletableFuture<String> expired = orchestrator.supply(PjbExecutionDescriptor.io("expired-op", Duration.ofMillis(25)), () -> {
                executed.set(true);
                return "SHOULD_NOT_RUN";
            });
            ExecutionException ex = assertThrows(ExecutionException.class, () -> expired.get(1, TimeUnit.SECONDS));
            assertTrue(ex.getCause() instanceof PjbExecutionTimedOutException);
            latch.countDown();
            assertEquals("BLOCKED", blocking.get(1, TimeUnit.SECONDS));
            Thread.sleep(150L);
            assertTrue(!executed.get());
        } finally {
            scheduler.shutdownNow();
            io.close();
            burst.close();
            externalIo.close();
            live.close();
            job.close();
        }
    }



    @Test
    void devePropagarContextoDeSigiloParaExecucaoAssincrona() throws Exception {
        PjbBoundedExecutorService io = new PjbBoundedExecutorService("test-io-", 2, true, Duration.ofSeconds(5), Duration.ofMillis(50));
        PjbBoundedExecutorService burst = new PjbBoundedExecutorService("test-burst-", 2, true, Duration.ofSeconds(5), Duration.ofMillis(50));
        PjbBoundedExecutorService externalIo = new PjbBoundedExecutorService("test-ext-", 2, true, Duration.ofSeconds(5), Duration.ofMillis(50));
        PjbBoundedExecutorService live = new PjbBoundedExecutorService("test-live-", 2, true, Duration.ofSeconds(5), Duration.ofMillis(50));
        PjbBoundedExecutorService job = new PjbBoundedExecutorService("test-job-", 2, true, Duration.ofSeconds(5), Duration.ofMillis(50));
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        PjbProcessoSigiloRlsContext context = new PjbProcessoSigiloRlsContext();
        try {
            context.bind(new ProcessoSigiloRlsEnvelope(
                    "processo",
                    java.util.List.of("DADOS_JUDICIAIS"),
                    "SIGILO_N2",
                    "SIGILO_N2",
                    "TJCE",
                    "UNID-77",
                    "TJCE:UNID-77:SIGILO_N2",
                    true,
                    false,
                    false
            ));
            PjbExecutionOrchestrator orchestrator = new PjbExecutionOrchestrator(new PjbBoundedExecutorProvider(io, burst, externalIo, live, job), scheduler, context);
            java.util.concurrent.atomic.AtomicReference<PjbProcessoSigiloRlsContext.SessionSettings> captured = new java.util.concurrent.atomic.AtomicReference<>();
            String result = orchestrator.supply(PjbExecutionDescriptor.io("sigilo-context-op", Duration.ofSeconds(1)), () -> {
                captured.set(context.currentOrDefault());
                return "OK";
            }).get(2, TimeUnit.SECONDS);
            assertEquals("OK", result);
            assertEquals("SIGILO_N2", captured.get().sigiloClearance());
            assertEquals("TJCE", captured.get().tribunalCode());
            assertEquals("UNID-77", captured.get().unitCode());
            assertEquals("TJCE:UNID-77:SIGILO_N2", captured.get().sigiloScope());
            assertEquals("SIGILO_N2", context.currentOrDefault().sigiloClearance());
        } finally {
            context.clear();
            scheduler.shutdownNow();
            io.close();
            burst.close();
            externalIo.close();
            live.close();
            job.close();
        }
    }

}
