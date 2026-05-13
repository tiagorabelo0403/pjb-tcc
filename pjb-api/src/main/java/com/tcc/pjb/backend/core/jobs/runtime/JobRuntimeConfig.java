package com.tcc.pjb.backend.core.jobs.runtime;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JobRuntimeConfig {

    @Bean
    public ExecutorService jobVirtualThreadExecutor(@Qualifier("pjbJobExecutorService") ExecutorService jobExecutorService) {
        return jobExecutorService;
    }

    @Bean
    public JobNotifySignal jobNotifySignal() {
        return new JobNotifySignal();
    }

    @Bean
    public JobInstanceIdProvider jobInstanceIdProvider() {
        String host;
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            host = "unknown";
        }
        String jvmName = ManagementFactory.getRuntimeMXBean().getName();
        String pid = jvmName != null && jvmName.contains("@") ? jvmName.substring(0, jvmName.indexOf('@')) : "0";
        return new JobInstanceIdProvider(host + "-" + pid);
    }
}
