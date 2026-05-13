package com.tcc.pjb.backend.platform.runtime;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.runtime.lifecycle")
public class PjbRuntimeLifecycleProperties {

    private boolean enabled = true;
    private boolean publishAvailabilityEvents = true;
    private boolean failReadyWhenDraining = true;
    private Duration drainQuietPeriod = Duration.ofSeconds(20);
    private Duration shutdownAwaitTimeout = Duration.ofSeconds(30);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isPublishAvailabilityEvents() {
        return publishAvailabilityEvents;
    }

    public void setPublishAvailabilityEvents(boolean publishAvailabilityEvents) {
        this.publishAvailabilityEvents = publishAvailabilityEvents;
    }

    public boolean isFailReadyWhenDraining() {
        return failReadyWhenDraining;
    }

    public void setFailReadyWhenDraining(boolean failReadyWhenDraining) {
        this.failReadyWhenDraining = failReadyWhenDraining;
    }

    public Duration getDrainQuietPeriod() {
        return drainQuietPeriod;
    }

    public void setDrainQuietPeriod(Duration drainQuietPeriod) {
        this.drainQuietPeriod = drainQuietPeriod;
    }

    public Duration getShutdownAwaitTimeout() {
        return shutdownAwaitTimeout;
    }

    public void setShutdownAwaitTimeout(Duration shutdownAwaitTimeout) {
        this.shutdownAwaitTimeout = shutdownAwaitTimeout;
    }
}
