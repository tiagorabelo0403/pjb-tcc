package com.tcc.pjb.backend.core.jobs.runtime;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;


@ConfigurationProperties(prefix = "pjb.jobs.dispatcher")
public class JobDispatcherProperties {

    private boolean enabled = true;
    private int claimBatchSize = 100;
    private long lockTtlSeconds = 120;
    private int maxParallel = 100;

    private int maxParallelPerType = 24;
    private int minParallelPerType = 1;

    private long batchJoinMillis = 350;
    private long adaptiveRebalanceMillis = 5000;

    private long backoffMinMillis = 1000;
    private long backoffMaxMillis = 10000;
    private long backoffStepMillis = 1000;

    private long defaultJobTypeBudgetMillis = 20000;
    private Map<String, Long> jobTypeBudgetMillis;

    private int maxParallelPerTenant = 0;
    private int maxParallelPerUf = 0;
    private int maxParallelPerOrgao = 0;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getClaimBatchSize() {
        return claimBatchSize;
    }

    public void setClaimBatchSize(int claimBatchSize) {
        this.claimBatchSize = claimBatchSize;
    }

    public long getLockTtlSeconds() {
        return lockTtlSeconds;
    }

    public void setLockTtlSeconds(long lockTtlSeconds) {
        this.lockTtlSeconds = lockTtlSeconds;
    }

    public int getMaxParallel() {
        return maxParallel;
    }

    public void setMaxParallel(int maxParallel) {
        this.maxParallel = maxParallel;
    }

    public int getMaxParallelPerType() {
        return maxParallelPerType;
    }

    public void setMaxParallelPerType(int maxParallelPerType) {
        this.maxParallelPerType = maxParallelPerType;
    }

    public int getMinParallelPerType() {
        return minParallelPerType;
    }

    public void setMinParallelPerType(int minParallelPerType) {
        this.minParallelPerType = minParallelPerType;
    }

    public long getBatchJoinMillis() {
        return batchJoinMillis;
    }

    public void setBatchJoinMillis(long batchJoinMillis) {
        this.batchJoinMillis = batchJoinMillis;
    }

    public long getAdaptiveRebalanceMillis() {
        return adaptiveRebalanceMillis;
    }

    public void setAdaptiveRebalanceMillis(long adaptiveRebalanceMillis) {
        this.adaptiveRebalanceMillis = adaptiveRebalanceMillis;
    }

    public long getBackoffMinMillis() {
        return backoffMinMillis;
    }

    public void setBackoffMinMillis(long backoffMinMillis) {
        this.backoffMinMillis = backoffMinMillis;
    }

    public long getBackoffMaxMillis() {
        return backoffMaxMillis;
    }

    public void setBackoffMaxMillis(long backoffMaxMillis) {
        this.backoffMaxMillis = backoffMaxMillis;
    }

    public long getBackoffStepMillis() {
        return backoffStepMillis;
    }

    public void setBackoffStepMillis(long backoffStepMillis) {
        this.backoffStepMillis = backoffStepMillis;
    }

    public long getDefaultJobTypeBudgetMillis() {
        return defaultJobTypeBudgetMillis;
    }

    public void setDefaultJobTypeBudgetMillis(long defaultJobTypeBudgetMillis) {
        this.defaultJobTypeBudgetMillis = defaultJobTypeBudgetMillis;
    }

    public Map<String, Long> getJobTypeBudgetMillis() {
        return jobTypeBudgetMillis;
    }

    public void setJobTypeBudgetMillis(Map<String, Long> jobTypeBudgetMillis) {
        this.jobTypeBudgetMillis = jobTypeBudgetMillis;
    }

    public long budgetMillisForType(String type) {
        return budgetMillisForType(type, 1d);
    }

    public long budgetMillisForType(String type, double multiplier) {
        long base;
        if (jobTypeBudgetMillis == null || jobTypeBudgetMillis.isEmpty()) {
            base = Math.max(100, defaultJobTypeBudgetMillis);
        } else {
            Long v = jobTypeBudgetMillis.get(type);
            if (v == null) {
                v = jobTypeBudgetMillis.get("*");
            }
            base = Math.max(100, v == null ? defaultJobTypeBudgetMillis : v);
        }
        double safeMultiplier = Math.max(0.5d, Math.min(4d, multiplier));
        return Math.max(100L, Math.round(base * safeMultiplier));
    }

    public int getMaxParallelPerTenant() {
        return maxParallelPerTenant;
    }

    public void setMaxParallelPerTenant(int maxParallelPerTenant) {
        this.maxParallelPerTenant = maxParallelPerTenant;
    }

    public int getMaxParallelPerUf() {
        return maxParallelPerUf;
    }

    public void setMaxParallelPerUf(int maxParallelPerUf) {
        this.maxParallelPerUf = maxParallelPerUf;
    }

    public int getMaxParallelPerOrgao() {
        return maxParallelPerOrgao;
    }

    public void setMaxParallelPerOrgao(int maxParallelPerOrgao) {
        this.maxParallelPerOrgao = maxParallelPerOrgao;
    }
}
