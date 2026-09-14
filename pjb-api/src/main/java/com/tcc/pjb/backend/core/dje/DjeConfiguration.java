package com.tcc.pjb.backend.core.dje;

import com.tcc.pjb.backend.core.guard.MockGuardAuditEvent;
import com.tcc.pjb.backend.core.guard.MockGuardEnvironmentQuery;
import com.tcc.pjb.backend.core.guard.MockGuardEnvironmentValidator;
import com.tcc.pjb.backend.core.guard.MockGuardViolation;
import com.tcc.pjb.backend.core.guard.MockGuardViolationException;
import com.tcc.pjb.backend.core.dje.domain.DjePartesNotificacaoResult;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "pjb.runtime.barrier.integrations", name = "dje", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(DjeProperties.class)
public class DjeConfiguration {

    @Bean
    @ConditionalOnMissingBean(DjeHttpClient.class)
    @ConditionalOnProperty(prefix = "pjb.dje", name = "mock-enabled", havingValue = "true", matchIfMissing = false)
    public DjeHttpClient djeHttpClient(MockGuardEnvironmentQuery mockGuardQuery,
                                       ApplicationEventPublisher eventPublisher) {
        if (mockGuardQuery.isRealEnvironment()) {
            MockGuardViolation violation = MockGuardViolation.of(
                    "dje", MockGuardEnvironmentValidator.DJE_MOCK_PROPERTY, mockGuardQuery.activeGuardProfile());
            mockGuardQuery.recordViolation("dje");
            eventPublisher.publishEvent(new MockGuardAuditEvent(this, violation));
            throw new MockGuardViolationException(violation);
        }
        return (tribunalCodigo, conteudo, tipoAto) -> new com.tcc.pjb.backend.core.dje.domain.DjeEnvioResult("MOCK");
    }

    // Fora de ambiente real: evita que o contexto inteiro deixe de subir só porque o DJE
    // não foi configurado (dormente por padrão, como as demais integrações judiciais).
    // Em prod/staging o fail-fast documentado em MockGuardStartupDjeIntegrationTest continua valendo.
    @Bean
    @ConditionalOnMissingBean(DjeHttpClient.class)
    @ConditionalOnExpression("!${pjb.mock-guard.real-environment:false}")
    public DjeHttpClient djeHttpClientNaoConfigurado() {
        return (tribunalCodigo, conteudo, tipoAto) -> {
            throw new IllegalStateException("dje_integracao_nao_configurada");
        };
    }

    @Bean
    @ConditionalOnMissingBean(DjePartesNotificacaoPort.class)
    @ConditionalOnProperty(prefix = "pjb.dje", name = "mock-enabled", havingValue = "true", matchIfMissing = false)
    public DjePartesNotificacaoPort djePartesNotificacaoPort(MockGuardEnvironmentQuery mockGuardQuery,
                                                              ApplicationEventPublisher eventPublisher) {
        if (mockGuardQuery.isRealEnvironment()) {
            MockGuardViolation violation = MockGuardViolation.of(
                    "dje", MockGuardEnvironmentValidator.DJE_MOCK_PROPERTY, mockGuardQuery.activeGuardProfile());
            mockGuardQuery.recordViolation("dje");
            eventPublisher.publishEvent(new MockGuardAuditEvent(this, violation));
            throw new MockGuardViolationException(violation);
        }
        return publicacao -> DjePartesNotificacaoResult.success(publicacao.getId(), "mock");
    }

    @Bean
    @ConditionalOnMissingBean(DjePartesNotificacaoPort.class)
    @ConditionalOnExpression("!${pjb.mock-guard.real-environment:false}")
    public DjePartesNotificacaoPort djePartesNotificacaoPortNaoConfigurado() {
        return publicacao -> {
            throw new IllegalStateException("dje_integracao_nao_configurada");
        };
    }
}
