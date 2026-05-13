package com.tcc.pjb.backend.platform.runtime;

import com.tcc.pjb.backend.configs.datasource.PjbProcessoSigiloRlsContext;
import com.tcc.pjb.backend.core.security.concurrent.PjbExecutionContextTaskDecorator;
import com.tcc.pjb.backend.platform.concurrent.PjbVirtualThreadSpine;
import java.net.http.HttpClient;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.task.SimpleAsyncTaskExecutorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskDecorator;

@Configuration
@EnableConfigurationProperties({PjbRuntimeAccelerationProperties.class, PjbRuntimePressureProperties.class, PjbRuntimeLifecycleProperties.class})
public class PjbRuntimeAccelerationConfiguration {

    @Bean
    public PjbRuntimeSizingPolicy.Footprint pjbRuntimeFootprint() {
        return PjbRuntimeSizingPolicy.detect();
    }

    @Bean(name = "pjbIoExecutorService", destroyMethod = "close")
    public PjbBoundedExecutorService pjbIoExecutorService(PjbRuntimeAccelerationProperties properties,
                                                          PjbRuntimeSizingPolicy.Footprint footprint) {
        PjbRuntimeAccelerationProperties.Lane lane = properties.getIo();
        return new PjbBoundedExecutorService(lane.getThreadNamePrefix(), resolveLaneConcurrency("io", lane, properties, footprint), lane.isRejectWhenSaturated(), lane.getTerminationTimeout(), lane.getSaturationAcquireTimeout());
    }

    @Bean(name = "pjbBurstExecutorService", destroyMethod = "close")
    public PjbBoundedExecutorService pjbBurstExecutorService(PjbRuntimeAccelerationProperties properties,
                                                             PjbRuntimeSizingPolicy.Footprint footprint) {
        PjbRuntimeAccelerationProperties.Lane lane = properties.getBurst();
        return new PjbBoundedExecutorService(lane.getThreadNamePrefix(), resolveLaneConcurrency("burst", lane, properties, footprint), lane.isRejectWhenSaturated(), lane.getTerminationTimeout(), lane.getSaturationAcquireTimeout());
    }

    @Bean(name = "pjbExternalIoExecutorService", destroyMethod = "close")
    public PjbBoundedExecutorService pjbExternalIoExecutorService(PjbRuntimeAccelerationProperties properties,
                                                                  PjbRuntimeSizingPolicy.Footprint footprint) {
        PjbRuntimeAccelerationProperties.Lane lane = properties.getExternalIo();
        return new PjbBoundedExecutorService(lane.getThreadNamePrefix(), resolveLaneConcurrency("external-io", lane, properties, footprint), lane.isRejectWhenSaturated(), lane.getTerminationTimeout(), lane.getSaturationAcquireTimeout());
    }

    @Bean(name = "pjbLiveExecutorService", destroyMethod = "close")
    public PjbBoundedExecutorService pjbLiveExecutorService(PjbRuntimeAccelerationProperties properties,
                                                            PjbRuntimeSizingPolicy.Footprint footprint) {
        PjbRuntimeAccelerationProperties.Lane lane = properties.getLive();
        return new PjbBoundedExecutorService(lane.getThreadNamePrefix(), resolveLaneConcurrency("live", lane, properties, footprint), lane.isRejectWhenSaturated(), lane.getTerminationTimeout(), lane.getSaturationAcquireTimeout());
    }

    @Bean(name = "pjbJobExecutorService", destroyMethod = "close")
    public PjbBoundedExecutorService pjbJobExecutorService(PjbRuntimeAccelerationProperties properties,
                                                           PjbRuntimeSizingPolicy.Footprint footprint) {
        PjbRuntimeAccelerationProperties.Lane lane = properties.getJob();
        return new PjbBoundedExecutorService(lane.getThreadNamePrefix(), resolveLaneConcurrency("job", lane, properties, footprint), lane.isRejectWhenSaturated(), lane.getTerminationTimeout(), lane.getSaturationAcquireTimeout());
    }

    @Bean(name = "pjbTimeoutScheduler", destroyMethod = "shutdown")
    public ScheduledExecutorService pjbTimeoutScheduler(PjbRuntimeSizingPolicy.Footprint footprint) {
        ThreadFactory threadFactory = Thread.ofPlatform().name("pjb-timeout-", 0).daemon(true).factory();
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(PjbRuntimeSizingPolicy.timeoutSchedulerPoolSize(footprint), threadFactory);
        executor.setRemoveOnCancelPolicy(true);
        executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        return executor;
    }

    @Bean(name = "taskExecutor")
    public AsyncTaskExecutor taskExecutor(SimpleAsyncTaskExecutorBuilder builder,
                                          PjbRuntimeAccelerationProperties properties,
                                          PjbRuntimeSizingPolicy.Footprint footprint,
                                          PjbProcessoSigiloRlsContext processoSigiloRlsContext) {
        return buildLaneExecutor(builder, "async", properties.getAsync(), properties, footprint, new PjbExecutionContextTaskDecorator(processoSigiloRlsContext));
    }

    @Bean(name = "pjbIoExecutor")
    public AsyncTaskExecutor pjbIoExecutor(SimpleAsyncTaskExecutorBuilder builder,
                                           PjbRuntimeAccelerationProperties properties,
                                           PjbRuntimeSizingPolicy.Footprint footprint,
                                           PjbProcessoSigiloRlsContext processoSigiloRlsContext) {
        return buildLaneExecutor(builder, "io", properties.getIo(), properties, footprint, new PjbExecutionContextTaskDecorator(processoSigiloRlsContext));
    }

    @Bean(name = "pjbBurstExecutor")
    public AsyncTaskExecutor pjbBurstExecutor(SimpleAsyncTaskExecutorBuilder builder,
                                              PjbRuntimeAccelerationProperties properties,
                                              PjbRuntimeSizingPolicy.Footprint footprint,
                                              PjbProcessoSigiloRlsContext processoSigiloRlsContext) {
        return buildLaneExecutor(builder, "burst", properties.getBurst(), properties, footprint, new PjbExecutionContextTaskDecorator(processoSigiloRlsContext));
    }

    @Bean(name = "pjbExternalIoExecutor")
    public AsyncTaskExecutor pjbExternalIoExecutor(SimpleAsyncTaskExecutorBuilder builder,
                                                   PjbRuntimeAccelerationProperties properties,
                                                   PjbRuntimeSizingPolicy.Footprint footprint,
                                                   PjbProcessoSigiloRlsContext processoSigiloRlsContext) {
        return buildLaneExecutor(builder, "external-io", properties.getExternalIo(), properties, footprint, new PjbExecutionContextTaskDecorator(processoSigiloRlsContext));
    }

    @Bean(name = "pjbLiveExecutor")
    public AsyncTaskExecutor pjbLiveExecutor(SimpleAsyncTaskExecutorBuilder builder,
                                             PjbRuntimeAccelerationProperties properties,
                                             PjbRuntimeSizingPolicy.Footprint footprint,
                                             PjbProcessoSigiloRlsContext processoSigiloRlsContext) {
        return buildLaneExecutor(builder, "live", properties.getLive(), properties, footprint, new PjbExecutionContextTaskDecorator(processoSigiloRlsContext));
    }

    @Bean(name = "pjbJobExecutor")
    public AsyncTaskExecutor pjbJobExecutor(SimpleAsyncTaskExecutorBuilder builder,
                                            PjbRuntimeAccelerationProperties properties,
                                            PjbRuntimeSizingPolicy.Footprint footprint,
                                            PjbProcessoSigiloRlsContext processoSigiloRlsContext) {
        return buildLaneExecutor(builder, "job", properties.getJob(), properties, footprint, new PjbExecutionContextTaskDecorator(processoSigiloRlsContext));
    }

    @Bean(name = "pjbSharedHttpClient")
    public HttpClient pjbSharedHttpClient(@Qualifier("pjbExternalIoExecutorService") PjbBoundedExecutorService executor,
                                          PjbRuntimeAccelerationProperties properties) {
        return HttpClient.newBuilder()
                .executor(executor)
                .version(properties.getHttp().isPreferHttp2() ? HttpClient.Version.HTTP_2 : HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(properties.getHttp().getConnectTimeout())
                .build();
    }

    private int resolveLaneConcurrency(String laneName,
                                       PjbRuntimeAccelerationProperties.Lane lane,
                                       PjbRuntimeAccelerationProperties properties,
                                       PjbRuntimeSizingPolicy.Footprint footprint) {
        return PjbRuntimeSizingPolicy.clampLaneConcurrency(laneName, lane.getConcurrencyLimit(), footprint, properties.getComponentRole());
    }

    private SimpleAsyncTaskExecutor buildLaneExecutor(SimpleAsyncTaskExecutorBuilder builder,
                                                      String laneName,
                                                      PjbRuntimeAccelerationProperties.Lane lane,
                                                      PjbRuntimeAccelerationProperties properties,
                                                      PjbRuntimeSizingPolicy.Footprint footprint,
                                                      TaskDecorator taskDecorator) {
        return PjbVirtualThreadSpine.newAsyncTaskExecutor(
                builder,
                lane.getThreadNamePrefix(),
                resolveLaneConcurrency(laneName, lane, properties, footprint),
                lane.isRejectWhenSaturated(),
                lane.getTerminationTimeout(),
                taskDecorator
        );
    }
}
