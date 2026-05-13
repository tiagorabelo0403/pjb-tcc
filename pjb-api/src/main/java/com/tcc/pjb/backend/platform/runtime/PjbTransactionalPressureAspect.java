package com.tcc.pjb.backend.platform.runtime;

import java.lang.reflect.Method;
import java.time.Duration;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Aspect
@Component
@Order(10)
public class PjbTransactionalPressureAspect {

    private final PjbTransactionalPressureTracker pressureTracker;
    private final ThreadLocal<Integer> nestingDepth = ThreadLocal.withInitial(() -> 0);

    public PjbTransactionalPressureAspect(PjbTransactionalPressureTracker pressureTracker) {
        this.pressureTracker = pressureTracker;
    }

    @Around("@annotation(org.springframework.transaction.annotation.Transactional) || @within(org.springframework.transaction.annotation.Transactional)")
    public Object aroundTransactional(ProceedingJoinPoint joinPoint) throws Throwable {
        Transactional transactional = resolveTransactional(joinPoint);
        if (transactional == null) {
            return joinPoint.proceed();
        }
        int depth = nestingDepth.get();
        nestingDepth.set(depth + 1);
        if (depth > 0) {
            try {
                return joinPoint.proceed();
            } finally {
                unwind(depth);
            }
        }
        PjbTransactionalBudget budget = resolveBudget(joinPoint);
        String operationName = operationName(joinPoint, budget);
        Duration budgetDuration = resolveBudgetDuration(transactional, budget);
        boolean criticalBudget = budget != null && budget.critical();
        PjbTransactionalPressureTracker.Handle handle = pressureTracker.start(operationName, transactional.readOnly(), transactional.propagation().name(), budgetDuration, criticalBudget);
        Throwable failure = null;
        try {
            return joinPoint.proceed();
        } catch (Throwable ex) {
            failure = ex;
            throw ex;
        } finally {
            pressureTracker.complete(handle, failure);
            unwind(depth);
        }
    }

    private void unwind(int previousDepth) {
        if (previousDepth <= 0) {
            nestingDepth.remove();
            return;
        }
        nestingDepth.set(previousDepth);
    }

    private String operationName(ProceedingJoinPoint joinPoint, PjbTransactionalBudget budget) {
        if (budget != null && budget.operation() != null && !budget.operation().isBlank()) {
            return budget.operation().trim();
        }
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String owner = method.getDeclaringClass() == null ? "unknown" : method.getDeclaringClass().getSimpleName();
        return owner + "." + method.getName();
    }

    private Duration resolveBudgetDuration(Transactional transactional, PjbTransactionalBudget budget) {
        Duration annotated = budget != null && budget.maxMillis() > 0L ? Duration.ofMillis(budget.maxMillis()) : null;
        Duration txTimeout = transactional != null && transactional.timeout() > 0 ? Duration.ofSeconds(transactional.timeout()) : null;
        if (annotated == null) {
            return txTimeout;
        }
        if (txTimeout == null) {
            return annotated;
        }
        return annotated.compareTo(txTimeout) <= 0 ? annotated : txTimeout;
    }

    private static Transactional resolveTransactional(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Transactional transactional = AnnotatedElementUtils.findMergedAnnotation(method, Transactional.class);
        if (transactional != null) {
            return transactional;
        }
        return AnnotatedElementUtils.findMergedAnnotation(method.getDeclaringClass(), Transactional.class);
    }

    private static PjbTransactionalBudget resolveBudget(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        PjbTransactionalBudget budget = AnnotatedElementUtils.findMergedAnnotation(method, PjbTransactionalBudget.class);
        if (budget != null) {
            return budget;
        }
        return AnnotatedElementUtils.findMergedAnnotation(method.getDeclaringClass(), PjbTransactionalBudget.class);
    }
}
