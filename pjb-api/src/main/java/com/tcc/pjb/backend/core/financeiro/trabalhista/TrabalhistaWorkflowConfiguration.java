package com.tcc.pjb.backend.core.financeiro.trabalhista;

import com.tcc.pjb.backend.modules.custas.api.GruCodigoBarrasGenerator;
import com.tcc.pjb.backend.modules.custas.domain.GruResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TrabalhistaWorkflowProperties.class)
public class TrabalhistaWorkflowConfiguration {

    @Bean(name = "gruTrabalhistaGenerator")
    @ConditionalOnMissingBean(name = "gruTrabalhistaGenerator")
    GruCodigoBarrasGenerator gruTrabalhistaGenerator() {
        return (tipo, valor, uf) -> new GruResult(
                "TRAB-" + tipo,
                "23791.00000.00000.000000.0000000000",
                "23790000000000000000000000000000000000000000",
                "TRAB-0001"
        );
    }
}
