package com.tcc.pjb.backend.platform.cluster;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@EnableConfigurationProperties(PjbClusterCoordinationProperties.class)
public class PjbClusterCoordinationConfig {

    @Bean
    public PjbClusterLockService pjbClusterLockService(PjbClusterCoordinationProperties properties,
                                                       ObjectProvider<StringRedisTemplate> redisProvider,
                                                       ObjectProvider<MeterRegistry> meterRegistryProvider) {
        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();
        PjbLocalClusterLockService local = new PjbLocalClusterLockService(properties.getKeyPrefix(), meterRegistry);
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis != null) {
            return new PjbHybridClusterLockService(new PjbRedisClusterLockService(redis, properties.getKeyPrefix(), meterRegistry), local);
        }
        return local;
    }
}
