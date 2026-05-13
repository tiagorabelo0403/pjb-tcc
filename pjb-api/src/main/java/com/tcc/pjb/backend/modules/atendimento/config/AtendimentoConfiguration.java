package com.tcc.pjb.backend.modules.atendimento.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({AtendimentoTosProperties.class, AtendimentoRetentionProperties.class})
public class AtendimentoConfiguration {
}
