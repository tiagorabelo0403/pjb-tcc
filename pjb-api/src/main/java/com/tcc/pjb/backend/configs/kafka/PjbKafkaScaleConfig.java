package com.tcc.pjb.backend.configs.kafka;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@ConditionalOnClass(ConcurrentKafkaListenerContainerFactory.class)
@EnableConfigurationProperties(PjbKafkaScaleProperties.class)
public class PjbKafkaScaleConfig {

    @Bean
    public DefaultErrorHandler pjbKafkaErrorHandler(PjbKafkaScaleProperties properties) {
        return new DefaultErrorHandler(new FixedBackOff(
                Math.max(1L, properties.getRetryBackoff().toMillis()),
                Math.max(0L, properties.getRetryAttempts())));
    }

    @Bean(name = "pjbKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<Object, Object> pjbKafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<Object, Object> consumerFactory,
            DefaultErrorHandler errorHandler,
            PjbKafkaScaleProperties properties) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        configurer.configure(factory, consumerFactory);
        factory.setCommonErrorHandler(errorHandler);
        factory.setConcurrency(Math.max(1, properties.getListenerConcurrency()));
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.BATCH);
        factory.getContainerProperties().setPollTimeout(Math.max(250L, properties.getPollTimeout().toMillis()));
        factory.getContainerProperties().setIdleBetweenPolls(Math.max(0L, properties.getIdleBetweenPolls().toMillis()));
        factory.getContainerProperties().setObservationEnabled(properties.isObservationEnabled());
        factory.getContainerProperties().setDeliveryAttemptHeader(true);
        return factory;
    }
}
