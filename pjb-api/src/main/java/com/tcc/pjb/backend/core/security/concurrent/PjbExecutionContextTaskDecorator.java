package com.tcc.pjb.backend.core.security.concurrent;

import com.tcc.pjb.backend.configs.datasource.PjbProcessoSigiloRlsContext;
import java.util.Objects;
import org.springframework.core.task.TaskDecorator;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

public class PjbExecutionContextTaskDecorator implements TaskDecorator {

    private final PjbProcessoSigiloRlsContext processoSigiloRlsContext;

    public PjbExecutionContextTaskDecorator(PjbProcessoSigiloRlsContext processoSigiloRlsContext) {
        this.processoSigiloRlsContext = Objects.requireNonNull(processoSigiloRlsContext, "processoSigiloRlsContext");
    }

    @Override
    public Runnable decorate(Runnable runnable) {
        SecurityContext capturedSecurityContext = SecurityContextHolder.getContext();
        PjbProcessoSigiloRlsContext.SessionSettings capturedSessionSettings = processoSigiloRlsContext.current();
        return () -> {
            SecurityContext previousSecurityContext = SecurityContextHolder.getContext();
            PjbProcessoSigiloRlsContext.SessionSettings previousSessionSettings = processoSigiloRlsContext.current();
            try {
                SecurityContextHolder.setContext(capturedSecurityContext);
                processoSigiloRlsContext.restore(capturedSessionSettings);
                runnable.run();
            } finally {
                processoSigiloRlsContext.restore(previousSessionSettings);
                SecurityContextHolder.setContext(previousSecurityContext);
            }
        };
    }
}
