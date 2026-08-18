package com.tcc.pjb.backend.configs.live;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
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
    @ConditionalOnMissingBean(LiveClusterBus.class)
    @ConditionalOnProperty(name = "pjb.live.cluster.enabled", havingValue = "false", matchIfMissing = true)
    NoOpLiveClusterBus noOpLiveClusterBus() {
        return new NoOpLiveClusterBus();
    }

    @Bean
    @ConditionalOnMissingBean(LiveClusterStateStore.class)
    @ConditionalOnProperty(name = "pjb.live.cluster.enabled", havingValue = "false", matchIfMissing = true)
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

    @Bean
    @ConditionalOnProperty(name = "pjb.live.cluster.enabled", havingValue = "true")
    SmartInitializingSingleton liveClusterRequiresDistributedBackend(ObjectProvider<LiveClusterBus> clusterBus,
                                                                     ObjectProvider<LiveClusterStateStore> stateStore) {
        return () -> {
            LiveClusterBus bus = clusterBus.getIfAvailable();
            LiveClusterStateStore store = stateStore.getIfAvailable();
            if (bus == null || !bus.enabled() || store == null || !store.distributed()) {
                throw new IllegalStateException(
                        "pjb.live.cluster.enabled=true exige RedisLiveClusterBus/RedisLiveClusterStateStore reais; "
                                + "fallback NoOp e permitido apenas com cluster desligado.");
            }
        };
    }
}
