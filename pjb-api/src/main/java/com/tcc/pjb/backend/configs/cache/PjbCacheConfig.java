package com.tcc.pjb.backend.configs.cache;

import java.time.Duration;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@EnableConfigurationProperties(PjbCacheProperties.class)
public class PjbCacheConfig {

    @Bean
    @ConditionalOnProperty(prefix = "pjb.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnProperty(name = "pjb.cache.redis.enabled", havingValue = "true")
    @ConditionalOnBean(RedisConnectionFactory.class)
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory,
                                          ObjectMapper objectMapper,
                                          PjbCacheProperties properties) {

        Duration defaultTtl = properties.getRedis() != null && properties.getRedis().getDefaultTtl() != null
                ? properties.getRedis().getDefaultTtl()
                : Duration.ofMinutes(15);

        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .entryTtl(defaultTtl)
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper)));

        RedisCacheManager.RedisCacheManagerBuilder builder = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(base);

        for (Map.Entry<String, Duration> entry : properties.getTtl().entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                continue;
            }
            Duration ttl = entry.getValue();
            if (ttl == null || ttl.isNegative() || ttl.isZero()) {
                continue;
            }
            builder.withCacheConfiguration(entry.getKey(), base.entryTtl(ttl));
        }

        return builder.build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "pjb.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnProperty(name = "pjb.cache.redis.enabled", havingValue = "false", matchIfMissing = true)
    public CacheManager localCacheManager(PjbCacheProperties properties) {
        return new PjbBoundedLocalCacheManager(properties);
    }
}
