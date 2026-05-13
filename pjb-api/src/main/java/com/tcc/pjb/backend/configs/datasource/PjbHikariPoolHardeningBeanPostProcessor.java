package com.tcc.pjb.backend.configs.datasource;

import com.tcc.pjb.backend.platform.runtime.PjbRuntimeSizingPolicy;
import com.zaxxer.hikari.HikariDataSource;
import java.util.Locale;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

@Component
public class PjbHikariPoolHardeningBeanPostProcessor implements BeanPostProcessor {

    private static final PjbRuntimeSizingPolicy.Footprint FOOTPRINT = PjbRuntimeSizingPolicy.detect();

    private final PjbDatasourceBudgetProperties budgetProperties;

    public PjbHikariPoolHardeningBeanPostProcessor(PjbDatasourceBudgetProperties budgetProperties) {
        this.budgetProperties = budgetProperties;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof HikariDataSource dataSource) {
            harden(dataSource, beanName, FOOTPRINT, budgetProperties);
        }
        return bean;
    }

    static void harden(HikariDataSource dataSource, String beanName) {
        harden(dataSource, beanName, FOOTPRINT, defaults());
    }

    static void harden(HikariDataSource dataSource, String beanName, PjbRuntimeSizingPolicy.Footprint footprint) {
        harden(dataSource, beanName, footprint, defaults());
    }

    static void harden(HikariDataSource dataSource,
                       String beanName,
                       PjbRuntimeSizingPolicy.Footprint footprint,
                       PjbDatasourceBudgetProperties budgetProperties) {
        if (dataSource == null) {
            return;
        }
        int maximumPoolSize = PjbRuntimeSizingPolicy.clampHikariMaximumPoolSize(beanName, dataSource.getMaximumPoolSize(), footprint);
        maximumPoolSize = budgetCeiling(beanName, maximumPoolSize, budgetProperties);
        int minimumIdle = PjbRuntimeSizingPolicy.clampHikariMinimumIdle(dataSource.getMinimumIdle(), maximumPoolSize);
        long connectionTimeout = Math.max(250L, dataSource.getConnectionTimeout());
        long validationTimeout = Math.max(250L, Math.min(connectionTimeout, Math.max(250L, dataSource.getValidationTimeout())));
        long maxLifetime = Math.max(30000L, dataSource.getMaxLifetime());
        long keepaliveTime = normalizeKeepalive(dataSource.getKeepaliveTime(), maxLifetime);
        long leakDetectionThreshold = normalizeLeakDetectionThreshold(dataSource.getLeakDetectionThreshold(), maxLifetime);
        dataSource.setMaximumPoolSize(maximumPoolSize);
        dataSource.setMinimumIdle(minimumIdle);
        dataSource.setConnectionTimeout(connectionTimeout);
        dataSource.setValidationTimeout(validationTimeout);
        dataSource.setMaxLifetime(maxLifetime);
        dataSource.setKeepaliveTime(keepaliveTime);
        dataSource.setLeakDetectionThreshold(leakDetectionThreshold);
        if (dataSource.getPoolName() == null || dataSource.getPoolName().isBlank()) {
            dataSource.setPoolName(beanName == null || beanName.isBlank() ? "pjb-pool" : beanName);
        }
    }

    public static int budgetCeiling(String beanName, int maximumPoolSize, PjbDatasourceBudgetProperties budgetProperties) {
        if (budgetProperties == null || !budgetProperties.isEnabled()) {
            return maximumPoolSize;
        }
        int absoluteCeiling = Math.max(1, budgetProperties.getPoolAbsoluteCeiling());
        int minimumFloor = Math.max(1, budgetProperties.getMinimumPoolFloor());
        int auxiliaryFloor = Math.max(1, Math.min(minimumFloor, budgetProperties.getAuxiliaryPoolFloor()));
        int roleBudget = resolveRoleBudget(budgetProperties);
        int budgetCeiling = switch (classify(beanName)) {
            case WRITE -> Math.max(minimumFloor, switch (normalizeRole(budgetProperties.getInstanceRole())) {
                case "api" -> roleBudget / 2;
                case "worker" -> Math.max(minimumFloor, roleBudget * 2 / 3);
                default -> roleBudget / 2;
            });
            case READ -> Math.max(minimumFloor, switch (normalizeRole(budgetProperties.getInstanceRole())) {
                case "api" -> roleBudget / 2;
                case "worker" -> roleBudget / 3;
                default -> roleBudget / 3;
            });
            case AUXILIARY -> Math.max(auxiliaryFloor, roleBudget / 5);
        };
        return Math.min(maximumPoolSize, Math.min(absoluteCeiling, budgetCeiling));
    }

    private static int resolveRoleBudget(PjbDatasourceBudgetProperties budgetProperties) {
        int databaseMaxConnections = Math.max(16, budgetProperties.getDatabaseMaxConnections());
        int reservedConnections = Math.max(0, Math.min(databaseMaxConnections - 1, budgetProperties.getReservedConnections()));
        int minimumFloor = Math.max(1, budgetProperties.getMinimumPoolFloor());
        int usableConnections = Math.max(minimumFloor, databaseMaxConnections - reservedConnections);
        String role = normalizeRole(budgetProperties.getInstanceRole());
        if ("mixed".equals(role)) {
            int instanceCount = Math.max(1, budgetProperties.getInstanceCount());
            return Math.max(minimumFloor, usableConnections / instanceCount);
        }
        int apiWeight = Math.max(1, budgetProperties.getApiWeight());
        int workerWeight = Math.max(1, budgetProperties.getWorkerWeight());
        int weightSum = apiWeight + workerWeight;
        int apiBudget = Math.max(minimumFloor, usableConnections * apiWeight / weightSum);
        int workerBudget = Math.max(minimumFloor, usableConnections - apiBudget);
        int instanceCount = "api".equals(role) ? Math.max(1, budgetProperties.getApiInstanceCount()) : Math.max(1, budgetProperties.getWorkerInstanceCount());
        int roleBudget = "api".equals(role) ? apiBudget : workerBudget;
        return Math.max(minimumFloor, roleBudget / instanceCount);
    }

    private static PoolKind classify(String beanName) {
        String normalized = beanName == null ? "" : beanName.toLowerCase(Locale.ROOT);
        if (normalized.contains("candidate") || normalized.contains("regional") || normalized.contains("failover") || normalized.contains("observability")) {
            return PoolKind.AUXILIARY;
        }
        if (normalized.contains("read")) {
            return PoolKind.READ;
        }
        if (normalized.contains("write") || normalized.contains("primary")) {
            return PoolKind.WRITE;
        }
        return PoolKind.AUXILIARY;
    }

    private static String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "mixed";
        }
        String normalized = role.toLowerCase(Locale.ROOT).trim();
        return switch (normalized) {
            case "api", "worker", "mixed" -> normalized;
            default -> "mixed";
        };
    }

    private static PjbDatasourceBudgetProperties defaults() {
        return new PjbDatasourceBudgetProperties();
    }

    private static long normalizeKeepalive(long keepaliveTime, long maxLifetime) {
        if (keepaliveTime <= 0L) {
            return 0L;
        }
        if (maxLifetime <= 30000L) {
            return 0L;
        }
        long ceiling = Math.max(0L, maxLifetime - 1000L);
        if (ceiling < 30000L) {
            return 0L;
        }
        return Math.min(Math.max(30000L, keepaliveTime), ceiling);
    }

    private static long normalizeLeakDetectionThreshold(long leakDetectionThreshold, long maxLifetime) {
        if (leakDetectionThreshold <= 0L) {
            return 0L;
        }
        long floor = 2000L;
        long normalized = Math.max(floor, leakDetectionThreshold);
        if (maxLifetime > floor && normalized >= maxLifetime) {
            return 0L;
        }
        return normalized;
    }

    private enum PoolKind {
        WRITE,
        READ,
        AUXILIARY
    }
}
