package com.tcc.pjb.backend.configs.cache;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Data;

@Data
@ConfigurationProperties(prefix = "pjb.cache")
public class PjbCacheProperties {

    private boolean enabled = true;

    private Local local = new Local();

    private Redis redis = new Redis();

    private Map<String, Duration> ttl = new LinkedHashMap<>();

    private Map<String, Long> maximumSize = new LinkedHashMap<>();

    public Duration resolveTtl(String cacheName) {
        Duration configured = cacheName == null ? null : ttl.get(cacheName);
        if (configured != null && !configured.isNegative() && !configured.isZero()) {
            return configured;
        }
        return local.getDefaultTtl();
    }

    public long resolveMaximumSize(String cacheName) {
        Long configured = cacheName == null ? null : maximumSize.get(cacheName);
        if (configured != null && configured > 0L) {
            return configured;
        }
        return Math.max(1L, local.getDefaultMaximumSize());
    }

    @Data
    public static class Local {

        private Duration defaultTtl = Duration.ofMinutes(15);

        private long defaultMaximumSize = 10_000L;
    }

    @Data
    public static class Redis {

        private boolean enabled = false;

        private Duration defaultTtl = Duration.ofMinutes(15);
    }
}
