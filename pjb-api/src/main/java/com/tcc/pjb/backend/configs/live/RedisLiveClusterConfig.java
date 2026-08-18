package com.tcc.pjb.backend.configs.live;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
@ConditionalOnBean(RedisConnectionFactory.class)
@ConditionalOnProperty(name = "pjb.live.cluster.enabled", havingValue = "true")
public class RedisLiveClusterConfig {

    @Bean
    public RedisLiveClusterBus redisLiveClusterBus(
            StringRedisTemplate redis,
            ObjectProvider<ObjectMapper> objectMapper,
            @Value("${pjb.live.cluster.channel-prefix:pjb:live:}") String channelPrefix,
            @Value("${pjb.live.cluster.node-id:}") String nodeId) {
        return new RedisLiveClusterBus(redis, objectMapper.getIfAvailable(ObjectMapper::new), channelPrefix, nodeId);
    }

    @Bean
    public RedisMessageListenerContainer pjbLiveRedisListenerContainer(RedisConnectionFactory connectionFactory,
                                                                       RedisLiveClusterBus listener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setRecoveryInterval(Duration.ofSeconds(5).toMillis());
        container.addMessageListener(listener, new PatternTopic(listener.pattern()));
        return container;
    }
}
