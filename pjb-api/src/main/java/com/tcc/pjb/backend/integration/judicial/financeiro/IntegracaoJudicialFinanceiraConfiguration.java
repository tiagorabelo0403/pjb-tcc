package com.tcc.pjb.backend.integration.judicial.financeiro;

import com.tcc.pjb.backend.integration.judicial.financeiro.domain.InfojudConsultaResponse;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.RenajudRestricaoResponse;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "pjb.runtime.barrier.integrations", name = "judicial-financeiro", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(IntegracaoJudicialFinanceiraProperties.class)
public class IntegracaoJudicialFinanceiraConfiguration {

    @Bean
    @ConditionalOnMissingBean
    SisbajudHttpClient sisbajudHttpClient(IntegracaoJudicialFinanceiraProperties properties) {
        return (cpf, valor, oficio) -> new com.tcc.pjb.backend.integration.judicial.financeiro.domain.SisbajudHttpResponse(
                properties.dryRun() ? "SISB-DRY-RUN" : "SISB-MOCK",
                "Bloqueio simulado para CPF/CNPJ " + cpf + " no valor " + valor
        );
    }

    @Bean
    @ConditionalOnMissingBean
    RenajudHttpClient renajudHttpClient(IntegracaoJudicialFinanceiraProperties properties) {
        return (placa, renavam, tipo) -> new RenajudRestricaoResponse(
                properties.dryRun() ? "RENA-DRY-RUN" : "RENA-MOCK",
                "Restrição simulada " + tipo + " para placa " + placa + " renavam " + renavam
        );
    }

    @Bean
    @ConditionalOnMissingBean
    InfojudHttpClient infojudHttpClient(IntegracaoJudicialFinanceiraProperties properties) {
        return cpfCnpj -> new InfojudConsultaResponse(
                properties.dryRun() ? "INFO-DRY-RUN" : "INFO-MOCK",
                "Consulta fiscal simulada para " + cpfCnpj
        );
    }
}
