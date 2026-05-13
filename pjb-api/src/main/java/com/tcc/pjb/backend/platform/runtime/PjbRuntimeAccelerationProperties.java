package com.tcc.pjb.backend.platform.runtime;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.runtime.acceleration")
public class PjbRuntimeAccelerationProperties {

    private String componentRole = "mixed";
    private final Lane async = new Lane(256, false, Duration.ofSeconds(30), Duration.ofSeconds(2), "pjb-async-");
    private final Lane io = new Lane(512, false, Duration.ofSeconds(30), Duration.ofSeconds(2), "pjb-io-");
    private final Lane burst = new Lane(1024, true, Duration.ofSeconds(20), Duration.ofMillis(250), "pjb-burst-");
    private final Lane externalIo = new Lane(192, true, Duration.ofSeconds(30), Duration.ofMillis(500), "pjb-http-");
    private final Lane live = new Lane(256, true, Duration.ofSeconds(20), Duration.ofMillis(250), "pjb-live-");
    private final Lane job = new Lane(768, true, Duration.ofSeconds(20), Duration.ofMillis(250), "pjb-job-");
    private final Http http = new Http();

    public String getComponentRole() {
        return componentRole;
    }

    public void setComponentRole(String componentRole) {
        this.componentRole = componentRole;
    }

    public Lane getAsync() {
        return async;
    }

    public Lane getIo() {
        return io;
    }

    public Lane getBurst() {
        return burst;
    }

    public Lane getExternalIo() {
        return externalIo;
    }

    public Lane getLive() {
        return live;
    }

    public Lane getJob() {
        return job;
    }

    public Http getHttp() {
        return http;
    }

    public static final class Lane {
        private int concurrencyLimit;
        private boolean rejectWhenSaturated;
        private Duration terminationTimeout;
        private Duration saturationAcquireTimeout;
        private String threadNamePrefix;

        public Lane() {
        }

        public Lane(int concurrencyLimit,
                    boolean rejectWhenSaturated,
                    Duration terminationTimeout,
                    Duration saturationAcquireTimeout,
                    String threadNamePrefix) {
            this.concurrencyLimit = concurrencyLimit;
            this.rejectWhenSaturated = rejectWhenSaturated;
            this.terminationTimeout = terminationTimeout;
            this.saturationAcquireTimeout = saturationAcquireTimeout;
            this.threadNamePrefix = threadNamePrefix;
        }

        public int getConcurrencyLimit() {
            return concurrencyLimit;
        }

        public void setConcurrencyLimit(int concurrencyLimit) {
            this.concurrencyLimit = concurrencyLimit;
        }

        public boolean isRejectWhenSaturated() {
            return rejectWhenSaturated;
        }

        public void setRejectWhenSaturated(boolean rejectWhenSaturated) {
            this.rejectWhenSaturated = rejectWhenSaturated;
        }

        public Duration getTerminationTimeout() {
            return terminationTimeout;
        }

        public void setTerminationTimeout(Duration terminationTimeout) {
            this.terminationTimeout = terminationTimeout;
        }

        public Duration getSaturationAcquireTimeout() {
            return saturationAcquireTimeout;
        }

        public void setSaturationAcquireTimeout(Duration saturationAcquireTimeout) {
            this.saturationAcquireTimeout = saturationAcquireTimeout;
        }

        public String getThreadNamePrefix() {
            return threadNamePrefix;
        }

        public void setThreadNamePrefix(String threadNamePrefix) {
            this.threadNamePrefix = threadNamePrefix;
        }
    }

    public static final class Http {
        private Duration connectTimeout = Duration.ofSeconds(5);
        private Duration requestTimeout = Duration.ofSeconds(20);
        private boolean preferHttp2 = true;

        public Duration getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public Duration getRequestTimeout() {
            return requestTimeout;
        }

        public void setRequestTimeout(Duration requestTimeout) {
            this.requestTimeout = requestTimeout;
        }

        public boolean isPreferHttp2() {
            return preferHttp2;
        }

        public void setPreferHttp2(boolean preferHttp2) {
            this.preferHttp2 = preferHttp2;
        }
    }
}
