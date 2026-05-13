package com.tcc.pjb.backend.core.comunicacao.judicial.hsm;

import java.net.http.HttpClient;
import java.util.concurrent.ExecutorService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "pjb.runtime.barrier.integrations", name = "hsm", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties({PjbHsmProperties.class, SefazNfeProperties.class})
public class PjbHsmConfiguration {

    @Bean(name = "hsmInterceptacaoHttpClient")
    public HttpClient hsmInterceptacaoHttpClient(PjbHardwareSecurityModule hsm,
                                                 @Qualifier("pjbExternalIoExecutorService") ExecutorService externalIoExecutor) {
        return HttpClient.newBuilder()
                .sslContext(hsm.getSslContext())
                .version(HttpClient.Version.HTTP_2)
                .executor(externalIoExecutor)
                .build();
    }
}
