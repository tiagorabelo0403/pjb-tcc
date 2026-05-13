package com.tcc.pjb.backend.service.outbox.observability;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PjbOutboxObservabilityProperties.class)
public class PjbOutboxObservabilityConfig {
}
