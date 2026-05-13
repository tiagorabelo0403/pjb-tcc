package com.tcc.pjb.backend.platform.concurrent;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.tcc.pjb.backend.core.security.concurrent.SecurityContextAwareExecutorService;

@Configuration
public class AiVirtualExecutorConfiguration {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService aiVirtualThreadExecutorService(@Qualifier("pjbBurstExecutorService") ExecutorService burstExecutorService) {
        return new SecurityContextAwareExecutorService(burstExecutorService);
    }

    @Bean(name = "aiMeshExecutor")
    public Executor aiMeshExecutor(@Qualifier("aiVirtualThreadExecutorService") ExecutorService aiVirtualThreadExecutorService) {
        return aiVirtualThreadExecutorService;
    }
}
