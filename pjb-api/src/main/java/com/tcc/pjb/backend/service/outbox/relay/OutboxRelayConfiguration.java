package com.tcc.pjb.backend.service.outbox.relay;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OutboxRelayProperties.class)
public class OutboxRelayConfiguration {
}
