package com.tcc.pjb.backend.integration.serpro.datavalid;

import java.net.http.HttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SerproDataValidProperties.class)
public class SerproDataValidConfiguration {

    @Bean
    @ConditionalOnMissingBean(CpfValidacaoPort.class)
    public CpfValidacaoPort cpfValidacaoStubClient() {
        return new CpfValidacaoStubClient();
    }

    @Bean
    @ConditionalOnProperty(name = "pjb.integrations.serpro.datavalid.enabled", havingValue = "true")
    public SerproDataValidOAuthTokenService serproDataValidOAuthTokenService(
            SerproDataValidProperties properties,
            @Qualifier("pjbSharedHttpClient") HttpClient httpClient) {
        return new SerproDataValidOAuthTokenService(
                properties.tokenUrl(),
                properties.consumerKey(),
                properties.consumerSecret(),
                httpClient
        );
    }

    @Bean
    @ConditionalOnProperty(name = "pjb.integrations.serpro.datavalid.enabled", havingValue = "true")
    public SerproDataValidHttpClient serproDataValidHttpClient(
            SerproDataValidProperties properties,
            SerproDataValidOAuthTokenService tokenService,
            @Qualifier("pjbSharedHttpClient") HttpClient httpClient) {
        return new SerproDataValidHttpClient(properties, tokenService, httpClient);
    }
}
