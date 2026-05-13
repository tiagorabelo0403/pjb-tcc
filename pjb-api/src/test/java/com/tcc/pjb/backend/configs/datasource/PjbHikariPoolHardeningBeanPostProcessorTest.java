package com.tcc.pjb.backend.configs.datasource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.platform.runtime.PjbRuntimeSizingPolicy;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;

class PjbHikariPoolHardeningBeanPostProcessorTest {

    @Test
    void shouldClampPoolValuesAndThresholds() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setMaximumPoolSize(2);
        dataSource.setMinimumIdle(7);
        dataSource.setConnectionTimeout(250L);
        dataSource.setValidationTimeout(250L);
        dataSource.setMaxLifetime(1000L);
        dataSource.setKeepaliveTime(999999L);
        dataSource.setLeakDetectionThreshold(100L);
        PjbHikariPoolHardeningBeanPostProcessor.harden(dataSource, "pjbWriteDataSource", new PjbRuntimeSizingPolicy.Footprint(8, 4096));
        assertEquals(2, dataSource.getMaximumPoolSize());
        assertEquals(2, dataSource.getMinimumIdle());
        assertEquals(250L, dataSource.getConnectionTimeout());
        assertEquals(250L, dataSource.getValidationTimeout());
        assertEquals(30000L, dataSource.getMaxLifetime());
        assertEquals(0L, dataSource.getKeepaliveTime());
        assertEquals(2000L, dataSource.getLeakDetectionThreshold());
        dataSource.close();
    }

    @Test
    void shouldClampOversizedPoolAgainstRuntimeFootprint() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setMaximumPoolSize(64);
        dataSource.setMinimumIdle(32);
        PjbHikariPoolHardeningBeanPostProcessor.harden(dataSource, "pjbWriteDataSource", new PjbRuntimeSizingPolicy.Footprint(2, 1024));
        assertEquals(8, dataSource.getMaximumPoolSize());
        assertEquals(2, dataSource.getMinimumIdle());
        dataSource.close();
    }

    @Test
    void shouldClampPoolAgainstDatabaseBudgetPerInstance() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setMaximumPoolSize(64);
        dataSource.setMinimumIdle(16);
        PjbDatasourceBudgetProperties budget = new PjbDatasourceBudgetProperties();
        budget.setDatabaseMaxConnections(120);
        budget.setReservedConnections(24);
        budget.setInstanceCount(4);
        budget.setPoolAbsoluteCeiling(20);
        budget.setMinimumPoolFloor(4);
        budget.setAuxiliaryPoolFloor(2);
        PjbHikariPoolHardeningBeanPostProcessor.harden(dataSource, "pjbWriteDataSource", new PjbRuntimeSizingPolicy.Footprint(16, 8192), budget);
        assertEquals(12, dataSource.getMaximumPoolSize());
        assertEquals(4, dataSource.getMinimumIdle());
        dataSource.close();
    }

    @Test
    void shouldApplyWorkerRoleBudgetMoreGenerouslyThanApiRole() {
        HikariDataSource apiDataSource = new HikariDataSource();
        apiDataSource.setMaximumPoolSize(40);
        apiDataSource.setMinimumIdle(10);
        HikariDataSource workerDataSource = new HikariDataSource();
        workerDataSource.setMaximumPoolSize(40);
        workerDataSource.setMinimumIdle(10);
        PjbDatasourceBudgetProperties apiBudget = new PjbDatasourceBudgetProperties();
        apiBudget.setInstanceRole("api");
        apiBudget.setDatabaseMaxConnections(240);
        apiBudget.setReservedConnections(48);
        apiBudget.setApiInstanceCount(2);
        apiBudget.setWorkerInstanceCount(2);
        apiBudget.setPoolAbsoluteCeiling(24);
        PjbDatasourceBudgetProperties workerBudget = new PjbDatasourceBudgetProperties();
        workerBudget.setInstanceRole("worker");
        workerBudget.setDatabaseMaxConnections(240);
        workerBudget.setReservedConnections(48);
        workerBudget.setApiInstanceCount(2);
        workerBudget.setWorkerInstanceCount(2);
        workerBudget.setPoolAbsoluteCeiling(24);
        PjbHikariPoolHardeningBeanPostProcessor.harden(apiDataSource, "pjbWriteDataSource", new PjbRuntimeSizingPolicy.Footprint(16, 8192), apiBudget);
        PjbHikariPoolHardeningBeanPostProcessor.harden(workerDataSource, "pjbWriteDataSource", new PjbRuntimeSizingPolicy.Footprint(16, 8192), workerBudget);
        assertThat(workerDataSource.getMaximumPoolSize()).isGreaterThan(apiDataSource.getMaximumPoolSize());
        apiDataSource.close();
        workerDataSource.close();
    }


    @Test
    void shouldExposeBudgetCeilingForRuntimePressureAndMetricsConsumers() {
        PjbDatasourceBudgetProperties budget = new PjbDatasourceBudgetProperties();
        budget.setEnabled(true);
        budget.setInstanceRole("api");
        budget.setDatabaseMaxConnections(120);
        budget.setReservedConnections(24);
        budget.setApiInstanceCount(4);
        budget.setWorkerInstanceCount(2);
        budget.setPoolAbsoluteCeiling(20);
        budget.setMinimumPoolFloor(4);
        budget.setAuxiliaryPoolFloor(2);

        int writeCeiling = PjbHikariPoolHardeningBeanPostProcessor.budgetCeiling("pjbWriteDataSource", 64, budget);
        int readCeiling = PjbHikariPoolHardeningBeanPostProcessor.budgetCeiling("pjbReadReplicaDataSource", 64, budget);
        int auxiliaryCeiling = PjbHikariPoolHardeningBeanPostProcessor.budgetCeiling("observabilityDataSource", 64, budget);

        assertEquals(4, writeCeiling);
        assertEquals(4, readCeiling);
        assertEquals(2, auxiliaryCeiling);
    }

}
