package com.tcc.pjb.backend.configs.datasource;

import com.tcc.pjb.backend.platform.runtime.PjbRuntimeSizingPolicy;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PjbDatasourceBudgetMetricsConfig {

    public PjbDatasourceBudgetMetricsConfig(MeterRegistry meterRegistry,
                                            Map<String, HikariDataSource> dataSources,
                                            PjbDatasourceBudgetProperties budgetProperties,
                                            PjbRuntimeSizingPolicy.Footprint footprint) {
        dataSources.forEach((beanName, dataSource) -> register(meterRegistry, beanName, dataSource, budgetProperties, footprint));
    }

    private void register(MeterRegistry registry,
                          String beanName,
                          HikariDataSource dataSource,
                          PjbDatasourceBudgetProperties budgetProperties,
                          PjbRuntimeSizingPolicy.Footprint footprint) {
        String poolRole = normalizePoolRole(beanName);
        String instanceRole = normalizeRole(budgetProperties.getInstanceRole());
        Gauge.builder("pjb.datasource.max_pool", dataSource, HikariDataSource::getMaximumPoolSize)
                .tag("datasource", beanName)
                .tag("pool_role", poolRole)
                .tag("instance_role", instanceRole)
                .register(registry);
        Gauge.builder("pjb.datasource.min_idle", dataSource, HikariDataSource::getMinimumIdle)
                .tag("datasource", beanName)
                .tag("pool_role", poolRole)
                .tag("instance_role", instanceRole)
                .register(registry);
        Gauge.builder("pjb.datasource.budget_ceiling", dataSource, ds -> PjbHikariPoolHardeningBeanPostProcessor.budgetCeiling(beanName, ds.getMaximumPoolSize(), budgetProperties))
                .tag("datasource", beanName)
                .tag("pool_role", poolRole)
                .tag("instance_role", instanceRole)
                .register(registry);
        Gauge.builder("pjb.datasource.cpu_memory_ceiling", dataSource, ds -> PjbRuntimeSizingPolicy.clampHikariMaximumPoolSize(beanName, ds.getMaximumPoolSize(), footprint))
                .tag("datasource", beanName)
                .tag("pool_role", poolRole)
                .tag("instance_role", instanceRole)
                .register(registry);
        Gauge.builder("pjb.datasource.active", dataSource, this::activeConnections)
                .tag("datasource", beanName)
                .tag("pool_role", poolRole)
                .tag("instance_role", instanceRole)
                .register(registry);
        Gauge.builder("pjb.datasource.awaiting", dataSource, this::awaitingThreads)
                .tag("datasource", beanName)
                .tag("pool_role", poolRole)
                .tag("instance_role", instanceRole)
                .register(registry);
        Gauge.builder("pjb.datasource.headroom", dataSource, ds -> Math.max(0, ds.getMaximumPoolSize() - activeConnections(ds)))
                .tag("datasource", beanName)
                .tag("pool_role", poolRole)
                .tag("instance_role", instanceRole)
                .register(registry);
    }

    private int activeConnections(HikariDataSource dataSource) {
        HikariPoolMXBean bean = dataSource.getHikariPoolMXBean();
        return bean == null ? 0 : Math.max(0, bean.getActiveConnections());
    }

    private int awaitingThreads(HikariDataSource dataSource) {
        HikariPoolMXBean bean = dataSource.getHikariPoolMXBean();
        return bean == null ? 0 : Math.max(0, bean.getThreadsAwaitingConnection());
    }

    private String normalizePoolRole(String beanName) {
        String normalized = beanName == null ? "auxiliary" : beanName.toLowerCase(Locale.ROOT);
        if (normalized.contains("read")) {
            return "read";
        }
        if (normalized.contains("write") || normalized.contains("primary")) {
            return "write";
        }
        return "auxiliary";
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "mixed";
        }
        String normalized = role.toLowerCase(Locale.ROOT).trim();
        return switch (normalized) {
            case "api", "worker", "mixed" -> normalized;
            default -> "mixed";
        };
    }
}
