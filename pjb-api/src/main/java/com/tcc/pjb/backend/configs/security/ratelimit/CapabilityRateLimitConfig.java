package com.tcc.pjb.backend.configs.security.ratelimit;

import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitProperties;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitStore;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.security.ratelimit.LocalSlidingWindowRateLimitStore;
import com.tcc.pjb.backend.platform.security.ratelimit.RedisSlidingWindowRateLimitStore;

@Configuration
@EnableConfigurationProperties(CapabilityRateLimitProperties.class)
public class CapabilityRateLimitConfig {

    @Bean
    @ConditionalOnProperty(name = "pjb.security.capability-ratelimit.store", havingValue = "redis")
    public CapabilityRateLimitStore redisCapabilityRateLimitStore(StringRedisTemplate redis) {
        return new RedisSlidingWindowRateLimitStore(redis);
    }

    @Bean
    @ConditionalOnMissingBean(CapabilityRateLimitStore.class)
    public CapabilityRateLimitStore localCapabilityRateLimitStore() {
        return new LocalSlidingWindowRateLimitStore();
    }

    @Bean
    @RefreshScope
    public CapabilityRateLimiter capabilityRateLimiter(CapabilityRateLimitProperties props,
                                                       CapabilityRateLimitStore store,
                                                       Clock pjbClock) {
        return new CapabilityRateLimiter(props, store, pjbClock);
    }
}
