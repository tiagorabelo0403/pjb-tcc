package com.tcc.pjb.backend.platform.runtime;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PjbRuntimeMetricsConfig {

    public PjbRuntimeMetricsConfig(MeterRegistry meterRegistry,
                                   Map<String, PjbBoundedExecutorService> executors,
                                   @Qualifier("pjbTimeoutScheduler") ScheduledExecutorService timeoutScheduler,
                                   PjbRuntimeDrainService drainService,
                                   PjbRuntimePressureService pressureService,
                                   PjbLivePressureService livePressureService,
                                   PjbKafkaPressureService kafkaPressureService) {
        executors.forEach((beanName, executor) -> register(meterRegistry, beanName, executor));
        registerTimeoutScheduler(meterRegistry, timeoutScheduler);
        Gauge.builder("pjb.runtime.draining", drainService, service -> service.isDraining() ? 1.0d : 0.0d).register(meterRegistry);
        Gauge.builder("pjb.runtime.ready_for_traffic", drainService, service -> service.readyForTraffic() ? 1.0d : 0.0d).register(meterRegistry);
        Gauge.builder("pjb.runtime.pressure_score", pressureService, service -> service.snapshot().pressureScore()).strongReference(true).register(meterRegistry);
        Gauge.builder("pjb.runtime.headroom_score", pressureService, service -> service.snapshot().headroomScore()).strongReference(true).register(meterRegistry);
        Gauge.builder("pjb.runtime.warming_up", pressureService, service -> service.snapshot().warmingUp() ? 1.0d : 0.0d).strongReference(true).register(meterRegistry);
        Gauge.builder("pjb.runtime.scheduler_degraded", pressureService, service -> service.snapshot().scheduler().degraded() ? 1.0d : 0.0d).strongReference(true).register(meterRegistry);
        Gauge.builder("pjb.runtime.scheduler_rising_fast", pressureService, service -> service.snapshot().scheduler().risingFast() ? 1.0d : 0.0d).strongReference(true).register(meterRegistry);
        Gauge.builder("pjb.runtime.scheduler_sustained", pressureService, service -> service.snapshot().scheduler().sustained() ? 1.0d : 0.0d).strongReference(true).register(meterRegistry);
        Gauge.builder("pjb.runtime.uptime_ms", pressureService, service -> service.snapshot().uptimeMillis()).strongReference(true).register(meterRegistry);
        Gauge.builder("pjb.runtime.pressure_rising_fast", pressureService, service -> service.snapshot().trend().risingFast() ? 1.0d : 0.0d).strongReference(true).register(meterRegistry);
        Gauge.builder("pjb.runtime.pressure_sustained", pressureService, service -> service.snapshot().trend().sustained() ? 1.0d : 0.0d).strongReference(true).register(meterRegistry);
        Gauge.builder("pjb.runtime.critical_lane_overloaded", pressureService, service -> service.snapshot().criticalOverloadedExecutorNames().size()).strongReference(true).register(meterRegistry);
        Gauge.builder("pjb.runtime.datasource_rising_fast", pressureService, service -> service.snapshot().risingDatasourceNames().size()).strongReference(true).register(meterRegistry);
        Gauge.builder("pjb.runtime.critical_datasource_tight_headroom", pressureService, service -> service.snapshot().criticalTightDatasourceNames().size()).strongReference(true).register(meterRegistry);
        Gauge.builder("pjb.runtime.critical_datasource_runaway", pressureService, service -> service.snapshot().criticalDatasourceRunaway() ? 1.0d : 0.0d).strongReference(true).register(meterRegistry);
        Gauge.builder("pjb.runtime.heap_usage_ratio", pressureService, service -> service.snapshot().memory() == null ? 0.0d : service.snapshot().memory().heapUsageRatio()).strongReference(true).register(meterRegistry);
        Gauge.builder("pjb.runtime.metaspace_usage_ratio", pressureService, service -> service.snapshot().memory() == null ? 0.0d : service.snapshot().memory().metaspaceUsageRatio()).strongReference(true).register(meterRegistry);
        Gauge.builder("pjb.runtime.direct_buffer_mib", pressureService, service -> service.snapshot().memory() == null ? 0.0d : service.snapshot().memory().directBufferUsedMiB()).strongReference(true).register(meterRegistry);
        Gauge.builder("pjb.runtime.heap_rising_fast", pressureService, service -> service.snapshot().memory() != null && service.snapshot().memory().heapRisingFast() ? 1.0d : 0.0d).strongReference(true).register(meterRegistry);
        Gauge.builder("pjb.runtime.critical_memory_runaway", pressureService, service -> service.snapshot().criticalMemoryRunaway() ? 1.0d : 0.0d).strongReference(true).register(meterRegistry);
        Gauge.builder("pjb.runtime.gc_pause_ratio", pressureService, service -> service.snapshot().gc() == null ? 0.0d : service.snapshot().gc().pauseRatio()).strongReference(true).register(meterRegistry);
        Gauge.builder("pjb.runtime.gc_average_pause_ms", pressureService, service -> service.snapshot().gc() == null ? 0.0d : service.snapshot().gc().averagePauseMillis()).strongReference(true).register(meterRegistry);
        Gauge.builder("pjb.runtime.gc_rising_fast", pressureService, service -> service.snapshot().gc() != null && service.snapshot().gc().risingFast() ? 1.0d : 0.0d).strongReference(true).register(meterRegistry);
        Gauge.builder("pjb.runtime.critical_gc_pressure", pressureService, service -> service.snapshot().criticalGcPressure() ? 1.0d : 0.0d).strongReference(true).register(meterRegistry);
        Gauge.builder("pjb.runtime.live_subscribers", livePressureService, service -> service.lastSnapshot().totalSubscribers()).strongReference(true).register(meterRegistry);
        Gauge.builder("pjb.runtime.live_active_topics", livePressureService, service -> service.lastSnapshot().activeTopics()).strongReference(true).register(meterRegistry);
        Gauge.builder("pjb.runtime.live_surge", livePressureService, service -> service.lastSnapshot().criticalSurge() ? 1.0d : 0.0d).strongReference(true).register(meterRegistry);
        Gauge.builder("pjb.runtime.kafka_degraded", kafkaPressureService, service -> service.lastSnapshot().degraded() ? 1.0d : 0.0d).strongReference(true).register(meterRegistry);
        Gauge.builder("pjb.runtime.kafka_critical", kafkaPressureService, service -> service.lastSnapshot().critical() ? 1.0d : 0.0d).strongReference(true).register(meterRegistry);
        Gauge.builder("pjb.runtime.kafka_buffer_available_ratio", kafkaPressureService, service -> service.lastSnapshot().bufferAvailableRatio()).strongReference(true).register(meterRegistry);
        Gauge.builder("pjb.runtime.kafka_requests_in_flight", kafkaPressureService, service -> service.lastSnapshot().requestsInFlight()).strongReference(true).register(meterRegistry);
    }

    private void register(MeterRegistry registry, String beanName, PjbBoundedExecutorService executor) {
        Gauge.builder("pjb.executor.capacity", executor, PjbBoundedExecutorService::concurrencyLimit).tag("executor", beanName).tag("prefix", executor.threadNamePrefix()).register(registry);
        Gauge.builder("pjb.executor.active", executor, PjbBoundedExecutorService::activeTasks).tag("executor", beanName).tag("prefix", executor.threadNamePrefix()).register(registry);
        Gauge.builder("pjb.executor.available", executor, PjbBoundedExecutorService::availablePermits).tag("executor", beanName).tag("prefix", executor.threadNamePrefix()).register(registry);
        Gauge.builder("pjb.executor.max_observed_active", executor, PjbBoundedExecutorService::maxObservedActiveTasks).tag("executor", beanName).tag("prefix", executor.threadNamePrefix()).register(registry);
        Gauge.builder("pjb.executor.acquire_wait_avg_ms", executor, PjbBoundedExecutorService::averageAcquireWaitMillis).tag("executor", beanName).tag("prefix", executor.threadNamePrefix()).register(registry);
        Gauge.builder("pjb.executor.utilization_ratio", executor, PjbBoundedExecutorService::utilizationRatio).tag("executor", beanName).tag("prefix", executor.threadNamePrefix()).register(registry);
        Gauge.builder("pjb.executor.consecutive_rejections", executor, PjbBoundedExecutorService::consecutiveRejections).tag("executor", beanName).tag("prefix", executor.threadNamePrefix()).register(registry);
        Gauge.builder("pjb.executor.last_rejection_age_ms", executor, PjbBoundedExecutorService::millisSinceLastRejection).tag("executor", beanName).tag("prefix", executor.threadNamePrefix()).strongReference(true).register(registry);
        Gauge.builder("pjb.executor.accepting_tasks", executor, value -> value.acceptingTasks() ? 1.0d : 0.0d).tag("executor", beanName).tag("prefix", executor.threadNamePrefix()).register(registry);
        FunctionCounter.builder("pjb.executor.submitted_total", executor, PjbBoundedExecutorService::submittedTasks).tag("executor", beanName).tag("prefix", executor.threadNamePrefix()).register(registry);
        FunctionCounter.builder("pjb.executor.completed_total", executor, PjbBoundedExecutorService::completedTasks).tag("executor", beanName).tag("prefix", executor.threadNamePrefix()).register(registry);
        FunctionCounter.builder("pjb.executor.rejected_total", executor, PjbBoundedExecutorService::rejectedTasks).tag("executor", beanName).tag("prefix", executor.threadNamePrefix()).register(registry);
        FunctionCounter.builder("pjb.executor.saturation_rejections_total", executor, PjbBoundedExecutorService::saturationRejections).tag("executor", beanName).tag("prefix", executor.threadNamePrefix()).register(registry);
    }

    private void registerTimeoutScheduler(MeterRegistry registry, ScheduledExecutorService timeoutScheduler) {
        if (timeoutScheduler instanceof ScheduledThreadPoolExecutor scheduler) {
            Gauge.builder("pjb.scheduler.queue_size", scheduler, value -> value.getQueue().size()).tag("scheduler", "pjbTimeoutScheduler").register(registry);
            Gauge.builder("pjb.scheduler.active", scheduler, ScheduledThreadPoolExecutor::getActiveCount).tag("scheduler", "pjbTimeoutScheduler").register(registry);
            Gauge.builder("pjb.scheduler.pool_size", scheduler, ScheduledThreadPoolExecutor::getPoolSize).tag("scheduler", "pjbTimeoutScheduler").register(registry);
            Gauge.builder("pjb.scheduler.utilization_ratio", scheduler, value -> {
                        int poolSize = Math.max(1, value.getPoolSize() > 0 ? value.getPoolSize() : value.getCorePoolSize());
                        return value.getActiveCount() / (double) poolSize;
                    })
                    .tag("scheduler", "pjbTimeoutScheduler")
                    .register(registry);
        }
    }
}
