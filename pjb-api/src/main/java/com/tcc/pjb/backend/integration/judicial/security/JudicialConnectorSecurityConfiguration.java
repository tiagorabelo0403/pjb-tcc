package com.tcc.pjb.backend.integration.judicial.security;

import com.tcc.pjb.backend.configs.datasource.PjbProcessoSigiloRlsContext;
import com.tcc.pjb.backend.core.security.concurrent.PjbExecutionContextTaskDecorator;
import com.tcc.pjb.backend.platform.concurrent.PjbVirtualThreadSpine;
import com.tcc.pjb.backend.platform.runtime.PjbRuntimeAccelerationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.task.SimpleAsyncTaskExecutorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;

@Configuration
@ConditionalOnProperty(prefix = "pjb.runtime.barrier.integrations", name = "judicial-security", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(JudicialConnectorSecurityProperties.class)
public class JudicialConnectorSecurityConfiguration {

    @Bean(name = "judicialConnectorSecurityExecutor")
    public AsyncTaskExecutor judicialConnectorSecurityExecutor(SimpleAsyncTaskExecutorBuilder builder,
                                                              PjbRuntimeAccelerationProperties properties,
                                                              PjbProcessoSigiloRlsContext processoSigiloRlsContext) {
        return PjbVirtualThreadSpine.newAsyncTaskExecutor(
                builder,
                "pjb-jc-sec-",
                properties.getExternalIo().getConcurrencyLimit(),
                properties.getExternalIo().isRejectWhenSaturated(),
                properties.getExternalIo().getTerminationTimeout(),
                new PjbExecutionContextTaskDecorator(processoSigiloRlsContext)
        );
    }
}
