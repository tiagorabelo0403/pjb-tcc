package com.tcc.pjb.backend.ai.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.tcc.pjb.backend.ai.common.AiModelClient;

@Configuration
public class AiClientsConfig {

    @Bean(name = "aiModelV1")
    public AiModelClient aiModelV1(AiModelClientFactory factory) {
        return factory.create("v1");
    }

    @Bean(name = "aiModelV2")
    public AiModelClient aiModelV2(AiModelClientFactory factory) {
        return factory.create("v2");
    }

    @Bean(name = "aiModelV3")
    public AiModelClient aiModelV3(AiModelClientFactory factory) {
        return factory.create("v3");
    }
}
