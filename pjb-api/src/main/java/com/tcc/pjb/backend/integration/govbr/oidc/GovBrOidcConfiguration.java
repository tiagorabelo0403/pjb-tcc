package com.tcc.pjb.backend.integration.govbr.oidc;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.util.concurrent.ExecutorService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "pjb.runtime.barrier.integrations", name = "govbr", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(GovBrOidcProperties.class)
public class GovBrOidcConfiguration {

  @Bean
  public HttpClient govBrHttpClient(GovBrOidcProperties props,
                                    @Qualifier("pjbExternalIoExecutorService") ExecutorService externalIoExecutor) {
    return HttpClient.newBuilder()
        .executor(externalIoExecutor)
        .connectTimeout(props.connectTimeout())
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();
  }

  @Bean
  public GovBrOidcClient govBrOidcClient(ObjectMapper objectMapper, GovBrOidcProperties props, @Qualifier("govBrHttpClient") HttpClient govBrHttpClient) {
    props.validateIfEnabled();
    return new GovBrOidcClient(objectMapper, props, govBrHttpClient);
  }
}
