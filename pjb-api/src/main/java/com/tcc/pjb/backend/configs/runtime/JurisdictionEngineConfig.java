package com.tcc.pjb.backend.configs.runtime;

import com.tcc.pjb.backend.model.entity.JurisdictionEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class JurisdictionEngineConfig {

    @Bean
    public JurisdictionEngine.Engine jurisdictionEngine() {
        return JurisdictionEngine.Engine.buildDefault();
    }
}
