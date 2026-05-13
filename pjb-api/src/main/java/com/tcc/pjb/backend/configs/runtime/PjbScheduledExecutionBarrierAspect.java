package com.tcc.pjb.backend.configs.runtime;

import java.lang.reflect.Method;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Aspect
@Component
@EnableConfigurationProperties(PjbRuntimeBarrierProperties.class)
public class PjbScheduledExecutionBarrierAspect {

    private static final Logger log = LoggerFactory.getLogger(PjbScheduledExecutionBarrierAspect.class);

    private final PjbRuntimeBarrierProperties properties;

    public PjbScheduledExecutionBarrierAspect(PjbRuntimeBarrierProperties properties) {
        this.properties = properties;
    }

    @Around("@annotation(scheduled)")
    public Object aroundScheduled(ProceedingJoinPoint joinPoint, Scheduled scheduled) throws Throwable {
        Class<?> targetType = joinPoint.getTarget() != null ? joinPoint.getTarget().getClass() : ((MethodSignature) joinPoint.getSignature()).getDeclaringType();
        if (isBlocked(targetType)) {
            Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
            log.info("[PJB-RUNTIME-BARRIER] Execução agendada bloqueada por feature flag. classe={} metodo={}", targetType.getName(), method.getName());
            return null;
        }
        return joinPoint.proceed();
    }

    boolean isBlocked(Class<?> type) {
        String name = type.getName();
        PjbRuntimeBarrierProperties.Scheduling scheduling = properties.getScheduling();
        if (!scheduling.isEnabled()) {
            return true;
        }
        if (name.startsWith("com.tcc.pjb.backend.modules.atendimento.")) {
            return !scheduling.isAtendimento();
        }
        if (name.startsWith("com.tcc.pjb.backend.service.ui.") || name.startsWith("com.tcc.pjb.backend.service.julgamento.live.") || name.startsWith("com.tcc.pjb.backend.service.secretariat.live.")) {
            return !scheduling.isUi();
        }
        if (name.startsWith("com.tcc.pjb.backend.service.calendar.")) {
            return !scheduling.isCalendar();
        }
        if (name.startsWith("com.tcc.pjb.backend.core.comunicacao.judicial.")) {
            return !scheduling.isComunicacaoJudicial();
        }
        if (name.startsWith("com.tcc.pjb.backend.service.cidadao.govbr.") || name.startsWith("com.tcc.pjb.backend.service.security.govbr.")) {
            return !scheduling.isGovbr();
        }
        if (name.startsWith("com.tcc.pjb.backend.integration.") || name.startsWith("com.tcc.pjb.backend.core.comunicacao.institucional.delivery.") || name.startsWith("com.tcc.pjb.backend.core.comunicacao.institucional.governance.")) {
            return !scheduling.isIntegracoesExternas();
        }
        if (name.startsWith("com.tcc.pjb.backend.core.digitalizacao.")) {
            return !scheduling.isDigitalizacao();
        }
        if (name.startsWith("com.tcc.pjb.backend.core.dje.")) {
            return !scheduling.isDje();
        }
        return !scheduling.isCore();
    }
}
