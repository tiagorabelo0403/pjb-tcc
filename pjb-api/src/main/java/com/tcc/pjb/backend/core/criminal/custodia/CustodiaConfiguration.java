package com.tcc.pjb.backend.core.criminal.custodia;

import com.tcc.pjb.backend.core.criminal.custodia.domain.BnmpConsultaResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class CustodiaConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "pjb.bnmp", name = "enabled", havingValue = "false", matchIfMissing = true)
    public BnmpConsultaService bnmpConsultaService() {
        return cpf -> BnmpConsultaResult.vazio();
    }
}
