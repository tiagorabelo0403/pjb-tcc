package com.tcc.pjb.backend.ai.legalai.config;

import com.tcc.pjb.backend.platform.concurrent.PjbVirtualThreadSpine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;

@Configuration
public class DreamingPollingExecutorConfiguration {

    @Bean(name = "dreamingPollingExecutor")
    public ExecutorService dreamingPollingExecutor() {
        return PjbVirtualThreadSpine.newPerTaskExecutor("pjb-dream-polling");
    }
}
