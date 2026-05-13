package com.tcc.pjb.backend.platform.cluster;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.cluster.coordination")
public class PjbClusterCoordinationProperties {

    private boolean enabled = true;
    private boolean schedulerSingletonEnabled = true;
    private String keyPrefix = "pjb:coord:";
    private Duration defaultLockTtl = Duration.ofSeconds(30);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isSchedulerSingletonEnabled() {
        return schedulerSingletonEnabled;
    }

    public void setSchedulerSingletonEnabled(boolean schedulerSingletonEnabled) {
        this.schedulerSingletonEnabled = schedulerSingletonEnabled;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public Duration getDefaultLockTtl() {
        return defaultLockTtl;
    }

    public void setDefaultLockTtl(Duration defaultLockTtl) {
        this.defaultLockTtl = defaultLockTtl;
    }
}
