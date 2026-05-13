package com.tcc.pjb.backend.platform.concurrent;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AiTelemetryExecutorConfiguration {

    @Bean(name = "aiTelemetryExecutor")
    public Executor aiTelemetryExecutor(@Qualifier("pjbIoExecutorService") ExecutorService ioExecutorService) {
        return ioExecutorService;
    }
}
