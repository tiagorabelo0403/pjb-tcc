package com.tcc.pjb.backend.ai.common.clients.resilience;

import java.util.Objects;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import com.tcc.pjb.backend.ai.common.AiModelClient;
import com.tcc.pjb.backend.ai.common.AiProviderException;

public final class ResilientAiModelClient implements AiModelClient {

    public static final String CIRCUIT_BREAKER_NAME = "ai-model";

    private final AiModelClient delegate;
    private final CircuitBreaker circuitBreaker;
    private final String provedor;

    public ResilientAiModelClient(AiModelClient delegate, CircuitBreaker circuitBreaker, String provedor) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.circuitBreaker = Objects.requireNonNull(circuitBreaker, "circuitBreaker");
        this.provedor = provedor == null || provedor.isBlank() ? "desconhecido" : provedor;
    }

    @Override
    public String generate(String prompt) {
        try {
            return circuitBreaker.executeCallable(() -> delegate.generate(prompt));
        } catch (CallNotPermittedException e) {
            throw new AiProviderException(provedor, "circuito aberto apos falhas consecutivas", e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new AiProviderException(provedor, "falha inesperada (" + e.getClass().getSimpleName() + ")", e);
        }
    }

    @Override
    public double[] embed(String text) {
        return delegate.embed(text);
    }

    @Override
    public void audit(String action, String detail) {
        delegate.audit(action, detail);
    }

    @Override
    public void setTimeout(long millis) {
        delegate.setTimeout(millis);
    }

    public AiModelClient delegate() {
        return delegate;
    }
}
