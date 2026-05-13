package com.tcc.pjb.backend.core.jobs.runtime;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.jobs.circuit")
public class JobCircuitBreakerProperties {

    private int windowSeconds = 60;
    private int failureThreshold = 10;
    private int pauseSeconds = 60;

    public int getWindowSeconds() {
        return windowSeconds;
    }

    public void setWindowSeconds(int windowSeconds) {
        this.windowSeconds = windowSeconds;
    }

    public int getFailureThreshold() {
        return failureThreshold;
    }

    public void setFailureThreshold(int failureThreshold) {
        this.failureThreshold = failureThreshold;
    }

    public int getPauseSeconds() {
        return pauseSeconds;
    }

    public void setPauseSeconds(int pauseSeconds) {
        this.pauseSeconds = pauseSeconds;
    }
}
