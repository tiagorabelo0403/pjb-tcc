package com.tcc.pjb.backend.core.kernel.recursal.governance;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.recursal.idempotency")
public class RecursalIdempotencyProperties {


    private Duration inProgressTtl = Duration.ofMinutes(2);


    private int optimisticRetryMaxAttempts = 4;


    private Duration optimisticRetryBackoff = Duration.ofMillis(40);

    public Duration getInProgressTtl() {
        return inProgressTtl;
    }

    public void setInProgressTtl(Duration inProgressTtl) {
        if (inProgressTtl == null || inProgressTtl.isZero() || inProgressTtl.isNegative()) {
            this.inProgressTtl = Duration.ofMinutes(2);
            return;
        }
        this.inProgressTtl = inProgressTtl;
    }

    public int getOptimisticRetryMaxAttempts() {
        return optimisticRetryMaxAttempts;
    }

    public void setOptimisticRetryMaxAttempts(int optimisticRetryMaxAttempts) {
        if (optimisticRetryMaxAttempts <= 0) {
            this.optimisticRetryMaxAttempts = 4;
            return;
        }
        this.optimisticRetryMaxAttempts = optimisticRetryMaxAttempts;
    }

    public Duration getOptimisticRetryBackoff() {
        return optimisticRetryBackoff;
    }

    public void setOptimisticRetryBackoff(Duration optimisticRetryBackoff) {
        if (optimisticRetryBackoff == null || optimisticRetryBackoff.isZero() || optimisticRetryBackoff.isNegative()) {
            this.optimisticRetryBackoff = Duration.ofMillis(40);
            return;
        }
        this.optimisticRetryBackoff = optimisticRetryBackoff;
    }
}
