package com.tcc.pjb.backend.core.eleitoral;

import com.tcc.pjb.backend.core.eleitoral.domain.PrestacaoContasSnapshot;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.LocalDate;
import java.util.List;

@Configuration
@EnableConfigurationProperties(EleitoralTseProperties.class)
public class EleitoralConfiguration {

    @Bean
    @ConditionalOnMissingBean(TseResultadoClient.class)
    public TseResultadoClient tseResultadoClient() {
        return List::of;
    }

    @Bean
    @ConditionalOnMissingBean(TseSpcaClient.class)
    public TseSpcaClient tseSpcaClient() {
        return (processoId, numeroCandidato, anoEleitoral) -> new PrestacaoContasSnapshot(
                processoId,
                numeroCandidato,
                anoEleitoral,
                "SEM_INTEGRACAO",
                null,
                "cliente SPCA não configurado"
        );
    }
}
