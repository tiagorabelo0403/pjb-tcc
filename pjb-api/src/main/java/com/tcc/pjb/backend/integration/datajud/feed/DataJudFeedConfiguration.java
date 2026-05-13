package com.tcc.pjb.backend.integration.datajud.feed;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "pjb.runtime.barrier.integrations", name = "datajud", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(DataJudFeedProperties.class)
public class DataJudFeedConfiguration {

    @Bean
    @ConditionalOnMissingBean(DataJudFeedHttpClient.class)
    public DataJudFeedHttpClient dataJudFeedHttpClient() {
        return entries -> { };
    }
}
