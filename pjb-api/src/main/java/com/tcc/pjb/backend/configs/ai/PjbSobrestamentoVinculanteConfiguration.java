package com.tcc.pjb.backend.configs.ai;

import com.tcc.pjb.backend.core.processo.tese.SobrestamentoVinculanteRetomadaPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PjbSobrestamentoVinculanteConfiguration {

    @Bean
    @ConditionalOnMissingBean(SobrestamentoVinculanteRetomadaPort.class)
    public SobrestamentoVinculanteRetomadaPort noOpSobrestamentoVinculanteRetomadaPort() {
        return (teseVinculanteId, resultado, ementa) -> {
        };
    }
}
