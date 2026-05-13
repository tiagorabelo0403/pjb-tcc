package com.tcc.pjb.backend.core.jobs.runtime;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.jobs.housekeeping")
public class JobHousekeepingProperties {

    private long staleLockSeconds = 300;
    private int deleteSucceededAfterDays = 30;

    public long getStaleLockSeconds() {
        return staleLockSeconds;
    }

    public void setStaleLockSeconds(long staleLockSeconds) {
        this.staleLockSeconds = staleLockSeconds;
    }

    public int getDeleteSucceededAfterDays() {
        return deleteSucceededAfterDays;
    }

    public void setDeleteSucceededAfterDays(int deleteSucceededAfterDays) {
        this.deleteSucceededAfterDays = deleteSucceededAfterDays;
    }
}
