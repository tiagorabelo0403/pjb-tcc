package com.tcc.pjb.backend.configs.live;

import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
@ConditionalOnBean(RedisConnectionFactory.class)
@ConditionalOnProperty(name = "pjb.live.cluster.enabled", havingValue = "true")
public class RedisLiveClusterConfig {

    @Bean
    @ConditionalOnBean(RedisLiveClusterBus.class)
    public RedisMessageListenerContainer pjbLiveRedisListenerContainer(RedisConnectionFactory connectionFactory,
                                                                       RedisLiveClusterBus listener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setRecoveryInterval(Duration.ofSeconds(5).toMillis());
        container.addMessageListener(listener, new PatternTopic(listener.pattern()));
        return container;
    }
}
