package com.tcc.pjb.backend.configs.datasource;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Role;
import org.springframework.stereotype.Component;

@Component
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@ConfigurationProperties(prefix = "pjb.datasource.budget")
public class PjbDatasourceBudgetProperties {

    private boolean enabled = true;
    private int databaseMaxConnections = 240;
    private int reservedConnections = 48;
    private int instanceCount = 4;
    private int apiInstanceCount = 2;
    private int workerInstanceCount = 2;
    private int apiWeight = 3;
    private int workerWeight = 5;
    private String instanceRole = "mixed";
    private int poolAbsoluteCeiling = 24;
    private int minimumPoolFloor = 4;
    private int auxiliaryPoolFloor = 2;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getDatabaseMaxConnections() {
        return databaseMaxConnections;
    }

    public void setDatabaseMaxConnections(int databaseMaxConnections) {
        this.databaseMaxConnections = databaseMaxConnections;
    }

    public int getReservedConnections() {
        return reservedConnections;
    }

    public void setReservedConnections(int reservedConnections) {
        this.reservedConnections = reservedConnections;
    }

    public int getInstanceCount() {
        return instanceCount;
    }

    public void setInstanceCount(int instanceCount) {
        this.instanceCount = instanceCount;
    }

    public int getApiInstanceCount() {
        return apiInstanceCount;
    }

    public void setApiInstanceCount(int apiInstanceCount) {
        this.apiInstanceCount = apiInstanceCount;
    }

    public int getWorkerInstanceCount() {
        return workerInstanceCount;
    }

    public void setWorkerInstanceCount(int workerInstanceCount) {
        this.workerInstanceCount = workerInstanceCount;
    }

    public int getApiWeight() {
        return apiWeight;
    }

    public void setApiWeight(int apiWeight) {
        this.apiWeight = apiWeight;
    }

    public int getWorkerWeight() {
        return workerWeight;
    }

    public void setWorkerWeight(int workerWeight) {
        this.workerWeight = workerWeight;
    }

    public String getInstanceRole() {
        return instanceRole;
    }

    public void setInstanceRole(String instanceRole) {
        this.instanceRole = instanceRole;
    }

    public int getPoolAbsoluteCeiling() {
        return poolAbsoluteCeiling;
    }

    public void setPoolAbsoluteCeiling(int poolAbsoluteCeiling) {
        this.poolAbsoluteCeiling = poolAbsoluteCeiling;
    }

    public int getMinimumPoolFloor() {
        return minimumPoolFloor;
    }

    public void setMinimumPoolFloor(int minimumPoolFloor) {
        this.minimumPoolFloor = minimumPoolFloor;
    }

    public int getAuxiliaryPoolFloor() {
        return auxiliaryPoolFloor;
    }

    public void setAuxiliaryPoolFloor(int auxiliaryPoolFloor) {
        this.auxiliaryPoolFloor = auxiliaryPoolFloor;
    }
}
