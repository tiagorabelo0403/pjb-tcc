package com.tcc.pjb.backend.ai.juridica.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.tcc.pjb.backend.ai.juridica.router.JuridicaAiVersionSelector;
import com.tcc.pjb.backend.ai.juridica.v1.IAJuridicaV1;
import com.tcc.pjb.backend.ai.juridica.v2.IAJuridicaV2;
import com.tcc.pjb.backend.ai.juridica.v3.IAJuridicaV3;

@Configuration
public class JuridicaAiSelectorConfiguration {

    @Bean
    public JuridicaAiVersionSelector juridicaAiVersionSelector(IAJuridicaV1 v1, IAJuridicaV2 v2, IAJuridicaV3 v3) {
        return new JuridicaAiVersionSelector(v1, v2, v3);
    }
}
