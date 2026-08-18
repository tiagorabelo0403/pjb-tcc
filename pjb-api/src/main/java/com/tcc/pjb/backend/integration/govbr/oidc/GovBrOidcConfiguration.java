package com.tcc.pjb.backend.integration.govbr.oidc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.guard.MockGuardAuditEvent;
import com.tcc.pjb.backend.core.guard.MockGuardEnvironmentQuery;
import com.tcc.pjb.backend.core.guard.MockGuardEnvironmentValidator;
import com.tcc.pjb.backend.core.guard.MockGuardViolation;
import com.tcc.pjb.backend.core.guard.MockGuardViolationException;
import java.net.http.HttpClient;
import java.util.concurrent.ExecutorService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
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
  public GovBrOidcClient govBrOidcClient(ObjectMapper objectMapper,
                                          GovBrOidcProperties props,
                                          @Qualifier("govBrHttpClient") HttpClient govBrHttpClient,
                                          MockGuardEnvironmentQuery mockGuardQuery,
                                          ApplicationEventPublisher eventPublisher) {
    if (props.mockEnabled() && mockGuardQuery.isRealEnvironment()) {
      MockGuardViolation violation = MockGuardViolation.of(
          "govbr", MockGuardEnvironmentValidator.GOVBR_MOCK_PROPERTY, mockGuardQuery.activeGuardProfile());
      mockGuardQuery.recordViolation("govbr");
      eventPublisher.publishEvent(new MockGuardAuditEvent(this, violation));
      throw new MockGuardViolationException(violation);
    }
    props.validateIfEnabled();
    return new GovBrOidcClient(objectMapper, props, govBrHttpClient);
  }
}
