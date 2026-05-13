package com.tcc.pjb.backend.core.security.concurrent;

import org.springframework.core.task.TaskDecorator;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityContextTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        SecurityContext captured = SecurityContextHolder.getContext();
        return () -> {
            SecurityContext previous = SecurityContextHolder.getContext();
            try {
                SecurityContextHolder.setContext(captured);
                runnable.run();
            } finally {
                SecurityContextHolder.setContext(previous);
            }
        };
    }
}
