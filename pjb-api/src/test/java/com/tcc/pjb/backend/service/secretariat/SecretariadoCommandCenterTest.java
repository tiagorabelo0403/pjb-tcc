package com.tcc.pjb.backend.service.secretariat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import com.tcc.pjb.backend.service.secretariat.orchestration.SecretariadoCommandCenter;
import com.tcc.pjb.backend.platform.runtime.execution.PjbExecutionOrchestrator;
import com.tcc.pjb.backend.platform.runtime.execution.PjbExecutionDescriptor;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class SecretariadoCommandCenterTest {

    @Test
    void gerarDossiePatrimonialMustDegradeWithoutBlockingOnExecutorSaturation() {
        Executor blackholeExecutor = new AbstractExecutorService() {
            private volatile boolean shutdown;

            @Override
            public void shutdown() {
                shutdown = true;
            }

            @Override
            public List<Runnable> shutdownNow() {
                shutdown = true;
                return Collections.emptyList();
            }

            @Override
            public boolean isShutdown() {
                return shutdown;
            }

            @Override
            public boolean isTerminated() {
                return shutdown;
            }

            @Override
            public boolean awaitTermination(long timeout, TimeUnit unit) {
                return shutdown;
            }

            @Override
            public void execute(Runnable command) {
            }
        };

        PjbExecutionOrchestrator executionOrchestrator = mock(PjbExecutionOrchestrator.class);
        when(executionOrchestrator.supply(org.mockito.Mockito.any(PjbExecutionDescriptor.class), org.mockito.Mockito.any())).thenAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(1);
            return CompletableFuture.supplyAsync(supplier);
        });
        SecretariadoCommandCenter service = new SecretariadoCommandCenter(
                provider((SecretariadoCommandCenter.SisbajudClient) alvo -> new SecretariadoCommandCenter.InfoSisbajud(100, true)),
                provider((SecretariadoCommandCenter.RenajudClient) alvo -> new SecretariadoCommandCenter.InfoRenajud(2, true)),
                provider((SecretariadoCommandCenter.InfojudClient) alvo -> new SecretariadoCommandCenter.InfoInfojud("2025", 3500)),
                executionOrchestrator
        );

        long started = System.nanoTime();
        SecretariadoCommandCenter.DossiePatrimonial dossie = service.gerarDossiePatrimonial(10L, "123.456.789-00");
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);

        assertEquals("SEM_INDICIOS_RELEVANTES", dossie.statusConsolidado());
        assertFalse(dossie.achouIndicioPatrimonial());
        assertFalse(elapsedMs > 5000, "o orçamento temporal controlado não pode se transformar em espera indefinida");
        ((AbstractExecutorService) blackholeExecutor).shutdownNow();
    }

    private <T> ObjectProvider<T> provider(T value) {
        return new ObjectProvider<>() {
            @Override
            public T getObject(Object... args) {
                return value;
            }

            @Override
            public T getIfAvailable() {
                return value;
            }

            @Override
            public T getIfAvailable(java.util.function.Supplier<T> defaultSupplier) {
                return value;
            }

            @Override
            public T getIfUnique() {
                return value;
            }

            @Override
            public T getIfUnique(java.util.function.Supplier<T> defaultSupplier) {
                return value;
            }

            @Override
            public T getObject() {
                return value;
            }

            @Override
            public java.util.Iterator<T> iterator() {
                return List.of(value).iterator();
            }

            @Override
            public java.util.stream.Stream<T> stream() {
                return List.of(value).stream();
            }

            @Override
            public java.util.stream.Stream<T> orderedStream() {
                return List.of(value).stream();
            }
        };
    }
}
