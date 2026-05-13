package com.tcc.pjb.backend.core.dje;

import com.tcc.pjb.backend.core.dje.domain.DjePartesNotificacaoResult;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "pjb.runtime.barrier.integrations", name = "dje", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(DjeProperties.class)
public class DjeConfiguration {

    @Bean
    @ConditionalOnMissingBean(DjeHttpClient.class)
    public DjeHttpClient djeHttpClient() {
        return (tribunalCodigo, conteudo, tipoAto) -> new com.tcc.pjb.backend.core.dje.domain.DjeEnvioResult("MOCK");
    }

    @Bean
    @ConditionalOnMissingBean(DjePartesNotificacaoPort.class)
    public DjePartesNotificacaoPort djePartesNotificacaoPort() {
        return publicacao -> DjePartesNotificacaoResult.success(publicacao.getId(), "mock");
    }
}
