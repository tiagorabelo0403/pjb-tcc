package com.tcc.pjb.backend.integration.serpro.datavalid;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.integrations.serpro.datavalid")
public record SerproDataValidProperties(
        boolean enabled,
        String consumerKey,
        String consumerSecret,
        String baseUrl,
        String tokenUrl,
        Duration connectTimeout,
        Duration readTimeout
) {
    public SerproDataValidProperties {
        connectTimeout = connectTimeout != null && !connectTimeout.isNegative() && !connectTimeout.isZero()
                ? connectTimeout : Duration.ofSeconds(5);
        readTimeout = readTimeout != null && !readTimeout.isNegative() && !readTimeout.isZero()
                ? readTimeout : Duration.ofSeconds(3);
    }
}
