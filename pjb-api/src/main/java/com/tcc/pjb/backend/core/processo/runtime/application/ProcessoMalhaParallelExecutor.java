package com.tcc.pjb.backend.core.processo.runtime.application;

import com.tcc.pjb.backend.platform.runtime.PjbBoundedExecutorProvider;
import java.util.concurrent.Callable;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

@Component
public class ProcessoMalhaParallelExecutor {

    private static final Duration AWAIT_TIMEOUT = Duration.ofSeconds(6);

    private final PjbBoundedExecutorProvider boundedExecutorProvider;

    public ProcessoMalhaParallelExecutor(PjbBoundedExecutorProvider boundedExecutorProvider) {
        this.boundedExecutorProvider = boundedExecutorProvider;
    }

    public <A, B> Dupla<A, B> executar2(String operacao,
                                        Callable<A> primeira,
                                        Callable<B> segunda) {
        Future<A> primeiraFuture = boundedExecutorProvider.burst().submit(primeira);
        Future<B> segundaFuture = boundedExecutorProvider.burst().submit(segunda);
        return new Dupla<>(await(primeiraFuture, operacao), await(segundaFuture, operacao));
    }

    public <A, B, C> Trio<A, B, C> executar3(String operacao,
                                             Callable<A> primeira,
                                             Callable<B> segunda,
                                             Callable<C> terceira) {
        Future<A> primeiraFuture = boundedExecutorProvider.burst().submit(primeira);
        Future<B> segundaFuture = boundedExecutorProvider.burst().submit(segunda);
        Future<C> terceiraFuture = boundedExecutorProvider.burst().submit(terceira);
        return new Trio<>(await(primeiraFuture, operacao), await(segundaFuture, operacao), await(terceiraFuture, operacao));
    }

    public <A, B, C, D> Quarteto<A, B, C, D> executar4(String operacao,
                                                       Supplier<A> primeira,
                                                       Supplier<B> segunda,
                                                       Supplier<C> terceira,
                                                       Supplier<D> quarta) {
        return executar4Interno(operacao, toCallable(primeira), toCallable(segunda), toCallable(terceira), toCallable(quarta));
    }

    private <A, B, C, D> Quarteto<A, B, C, D> executar4Interno(String operacao,
                                                              Callable<A> primeira,
                                                              Callable<B> segunda,
                                                              Callable<C> terceira,
                                                              Callable<D> quarta) {
        Future<A> primeiraFuture = boundedExecutorProvider.burst().submit(primeira);
        Future<B> segundaFuture = boundedExecutorProvider.burst().submit(segunda);
        Future<C> terceiraFuture = boundedExecutorProvider.burst().submit(terceira);
        Future<D> quartaFuture = boundedExecutorProvider.burst().submit(quarta);
        return new Quarteto<>(
                await(primeiraFuture, operacao),
                await(segundaFuture, operacao),
                await(terceiraFuture, operacao),
                await(quartaFuture, operacao)
        );
    }



    public <A, B, C, D, E> Quinteto<A, B, C, D, E> executar5(String operacao,
                                                              Supplier<A> primeira,
                                                              Supplier<B> segunda,
                                                              Supplier<C> terceira,
                                                              Supplier<D> quarta,
                                                              Supplier<E> quinta) {
        return executar5Interno(operacao, toCallable(primeira), toCallable(segunda), toCallable(terceira), toCallable(quarta), toCallable(quinta));
    }

    private <A, B, C, D, E> Quinteto<A, B, C, D, E> executar5Interno(String operacao,
                                                                     Callable<A> primeira,
                                                                     Callable<B> segunda,
                                                                     Callable<C> terceira,
                                                                     Callable<D> quarta,
                                                                     Callable<E> quinta) {
        Future<A> primeiraFuture = boundedExecutorProvider.burst().submit(primeira);
        Future<B> segundaFuture = boundedExecutorProvider.burst().submit(segunda);
        Future<C> terceiraFuture = boundedExecutorProvider.burst().submit(terceira);
        Future<D> quartaFuture = boundedExecutorProvider.burst().submit(quarta);
        Future<E> quintaFuture = boundedExecutorProvider.burst().submit(quinta);
        return new Quinteto<>(
                await(primeiraFuture, operacao),
                await(segundaFuture, operacao),
                await(terceiraFuture, operacao),
                await(quartaFuture, operacao),
                await(quintaFuture, operacao)
        );
    }

    private <T> Callable<T> toCallable(Supplier<T> supplier) {
        return supplier::get;
    }

    private <T> T await(Future<T> future, String operacao) {
        try {
            return future.get(AWAIT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Operação paralela interrompida: " + operacao, e);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new IllegalStateException("Operação paralela excedeu o timeout controlado: " + operacao, e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Operação paralela falhou: " + operacao, e.getCause());
        }
    }

    public record Dupla<A, B>(A primeiro, B segundo) {
    }

    public record Trio<A, B, C>(A primeiro, B segundo, C terceiro) {
    }

    public record Quarteto<A, B, C, D>(A primeiro, B segundo, C terceiro, D quarto) {
    }

    public record Quinteto<A, B, C, D, E>(A primeiro, B segundo, C terceiro, D quarto, E quinto) {
    }
}
