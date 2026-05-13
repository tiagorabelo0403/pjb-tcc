package com.tcc.pjb.backend.configs.datasource;

import java.lang.reflect.Method;
import java.util.Objects;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Aspect
@Component
@ConditionalOnProperty(prefix = "pjb.datasource.routing", name = "enabled", havingValue = "true")
public class PjbPrimaryReadPreferenceAspect {

    private final PjbDataSourceRoutingProperties properties;
    private final PjbPrimaryReadPreferenceContext primaryReadPreferenceContext;

    public PjbPrimaryReadPreferenceAspect(PjbDataSourceRoutingProperties properties,
                                          PjbPrimaryReadPreferenceContext primaryReadPreferenceContext) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.primaryReadPreferenceContext = Objects.requireNonNull(primaryReadPreferenceContext, "primaryReadPreferenceContext");
    }

    @Around("@annotation(org.springframework.transaction.annotation.Transactional) || @within(org.springframework.transaction.annotation.Transactional)")
    public Object aroundTransactional(ProceedingJoinPoint joinPoint) throws Throwable {
        Transactional transactional = resolveTransactional(joinPoint);
        Object result = joinPoint.proceed();
        if (transactional != null && !transactional.readOnly()) {
            preferPrimaryAfterCommit();
        }
        return result;
    }

    private void preferPrimaryAfterCommit() {
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    primaryReadPreferenceContext.preferPrimaryFor(properties.getReadYourWritesWindow());
                }
            });
            return;
        }
        primaryReadPreferenceContext.preferPrimaryFor(properties.getReadYourWritesWindow());
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
}
