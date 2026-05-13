package com.tcc.pjb.backend.platform.cluster;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Objects;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class PjbClusterSingletonTaskAspect {

    private static final Logger log = LoggerFactory.getLogger(PjbClusterSingletonTaskAspect.class);

    private final PjbClusterCoordinationProperties properties;
    private final PjbClusterLockService lockService;

    public PjbClusterSingletonTaskAspect(PjbClusterCoordinationProperties properties,
                                         PjbClusterLockService lockService) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.lockService = Objects.requireNonNull(lockService, "lockService");
    }

    @Around("@annotation(task)")
    public Object around(ProceedingJoinPoint joinPoint, PjbClusterSingletonTask task) throws Throwable {
        if (!properties.isEnabled() || !properties.isSchedulerSingletonEnabled()) {
            return joinPoint.proceed();
        }
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String key = task.key().isBlank()
                ? method.getDeclaringClass().getSimpleName() + "." + method.getName()
                : task.key();
        Duration ttl = parseTtl(task.ttl(), properties.getDefaultLockTtl());
        PjbClusterLockService.Lease lease = lockService.tryAcquire(key, ttl).orElse(null);
        if (lease == null) {
            log.debug("Execução cluster-singleton em contenção: {}", key);
            return defaultValue(method.getReturnType());
        }
        try (lease) {
            return joinPoint.proceed();
        }
    }

    private static Duration parseTtl(String raw, Duration fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Duration.parse(raw.trim());
        } catch (Exception ex) {
            return fallback;
        }
    }

    private static Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (boolean.class.equals(returnType)) {
            return false;
        }
        if (char.class.equals(returnType)) {
            return '\0';
        }
        if (byte.class.equals(returnType) || short.class.equals(returnType) || int.class.equals(returnType)) {
            return 0;
        }
        if (long.class.equals(returnType)) {
            return 0L;
        }
        if (float.class.equals(returnType)) {
            return 0f;
        }
        if (double.class.equals(returnType)) {
            return 0d;
        }
        return null;
    }
}
