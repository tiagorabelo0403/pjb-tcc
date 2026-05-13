package com.tcc.pjb.backend.configs.live;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration(proxyBeanMethods = false)
public class LiveClusterStateStoreConfiguration {

    @Bean
    @ConditionalOnMissingBean(LiveClusterStateStore.class)
    NoOpLiveClusterStateStore noOpLiveClusterStateStore() {
        return new NoOpLiveClusterStateStore();
    }

    @Bean
    @Primary
    @ConditionalOnBean(StringRedisTemplate.class)
    @ConditionalOnProperty(name = "pjb.live.cluster.enabled", havingValue = "true")
    RedisLiveClusterStateStore redisLiveClusterStateStore(StringRedisTemplate redis,
                                                          ObjectProvider<ObjectMapper> objectMapper,
                                                          @Value("${pjb.live.cluster.key-prefix:pjb:live:cluster:}") String configuredKeyPrefix) {
        ObjectMapper mapper = objectMapper.getIfAvailable(ObjectMapper::new);
        return new RedisLiveClusterStateStore(redis, mapper, configuredKeyPrefix);
    }
}
