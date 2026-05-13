package com.tcc.pjb.backend.configs.kafka;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.kafka.scale")
public class PjbKafkaScaleProperties {

    private int listenerConcurrency = 3;
    private Duration pollTimeout = Duration.ofSeconds(2);
    private Duration idleBetweenPolls = Duration.ofMillis(100);
    private Duration retryBackoff = Duration.ofSeconds(1);
    private long retryAttempts = 3;
    private boolean observationEnabled = true;

    public int getListenerConcurrency() {
        return listenerConcurrency;
    }

    public void setListenerConcurrency(int listenerConcurrency) {
        this.listenerConcurrency = listenerConcurrency;
    }

    public Duration getPollTimeout() {
        return pollTimeout;
    }

    public void setPollTimeout(Duration pollTimeout) {
        this.pollTimeout = pollTimeout;
    }

    public Duration getIdleBetweenPolls() {
        return idleBetweenPolls;
    }

    public void setIdleBetweenPolls(Duration idleBetweenPolls) {
        this.idleBetweenPolls = idleBetweenPolls;
    }

    public Duration getRetryBackoff() {
        return retryBackoff;
    }

    public void setRetryBackoff(Duration retryBackoff) {
        this.retryBackoff = retryBackoff;
    }

    public long getRetryAttempts() {
        return retryAttempts;
    }

    public void setRetryAttempts(long retryAttempts) {
        this.retryAttempts = retryAttempts;
    }

    public boolean isObservationEnabled() {
        return observationEnabled;
    }

    public void setObservationEnabled(boolean observationEnabled) {
        this.observationEnabled = observationEnabled;
    }
}
