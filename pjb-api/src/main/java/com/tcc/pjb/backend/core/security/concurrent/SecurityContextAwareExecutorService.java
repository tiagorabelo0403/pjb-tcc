package com.tcc.pjb.backend.core.security.concurrent;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityContextAwareExecutorService extends AbstractExecutorService {

    private final ExecutorService delegate;

    public SecurityContextAwareExecutorService(ExecutorService delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
        SecurityContext captured = SecurityContextHolder.getContext();
        delegate.execute(() -> {
            SecurityContext previous = SecurityContextHolder.getContext();
            try {
                SecurityContextHolder.setContext(captured);
                command.run();
            } finally {
                SecurityContextHolder.setContext(previous);
            }
        });
    }
}
