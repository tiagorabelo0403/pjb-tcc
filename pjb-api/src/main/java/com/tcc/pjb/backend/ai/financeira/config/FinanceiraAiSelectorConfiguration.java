package com.tcc.pjb.backend.ai.financeira.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.tcc.pjb.backend.ai.financeira.router.FinanceiraAiVersionSelector;
import com.tcc.pjb.backend.ai.financeira.v1.IAFinanceiraV1;
import com.tcc.pjb.backend.ai.financeira.v2.IAFinanceiraV2;
import com.tcc.pjb.backend.ai.financeira.v3.IAFinanceiraV3;
import com.tcc.pjb.backend.financial.ai.FinancialAiResponseFactory;

@Configuration
public class FinanceiraAiSelectorConfiguration {

    @Bean
    public FinanceiraAiVersionSelector financeiraAiVersionSelector(IAFinanceiraV1 v1, IAFinanceiraV2 v2, IAFinanceiraV3 v3, FinancialAiResponseFactory responseFactory) {
        return new FinanceiraAiVersionSelector(v1, v2, v3, responseFactory);
    }
}
