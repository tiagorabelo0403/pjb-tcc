package com.tcc.pjb.backend.configs.runtime;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PjbRuntimeBarrierProperties.class)
@EnableScheduling
@ConditionalOnProperty(name = "pjb.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class PjbSchedulingActivationConfiguration {
}
