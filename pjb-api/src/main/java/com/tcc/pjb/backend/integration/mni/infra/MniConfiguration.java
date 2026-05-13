package com.tcc.pjb.backend.integration.mni.infra;

import com.tcc.pjb.backend.integration.mni.domain.MniHttpResponse;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "pjb.runtime.barrier.integrations", name = "mni", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(MniRemessaProperties.class)
public class MniConfiguration {

    @Bean
    @ConditionalOnMissingBean(MniHttpClient.class)
    public MniHttpClient mniHttpClient() {
        return (tribunalDestino, xml) -> new MniHttpResponse("MNI-" + Math.abs((tribunalDestino + ':' + xml).hashCode()));
    }
}
