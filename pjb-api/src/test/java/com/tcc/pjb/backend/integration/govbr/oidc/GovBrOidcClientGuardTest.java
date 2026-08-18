package com.tcc.pjb.backend.integration.govbr.oidc;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.guard.MockGuardEnvironmentQuery;
import com.tcc.pjb.backend.core.guard.MockGuardProfile;
import com.tcc.pjb.backend.core.guard.MockGuardViolationException;
import java.net.http.HttpClient;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class GovBrOidcClientGuardTest {

    private static GovBrOidcProperties propsWithMock(boolean mockEnabled) {
        return new GovBrOidcProperties(
                false, mockEnabled,
                null, null, null, null,
                "client-id", null,
                "https://localhost/callback", null,
                null, null, null,
                null, null,
                Duration.ofSeconds(4), Duration.ofSeconds(6), Duration.ofMinutes(5)
        );
    }

    private static MockGuardEnvironmentQuery queryWith(boolean isReal, MockGuardProfile profile) {
        MockGuardEnvironmentQuery q = mock(MockGuardEnvironmentQuery.class);
        when(q.isRealEnvironment()).thenReturn(isReal);
        when(q.activeGuardProfile()).thenReturn(profile);
        return q;
    }

    @Test
    void mockFalse_naoDisparagGuard() {
        GovBrOidcConfiguration config = new GovBrOidcConfiguration();
        GovBrOidcProperties props = propsWithMock(false);
        MockGuardEnvironmentQuery query = queryWith(false, MockGuardProfile.DEV);
        HttpClient httpClient = mock(HttpClient.class);
        ObjectMapper om = new ObjectMapper();

        // mock=false -> validateIfEnabled() runs; enabled=false -> passes validation
        assertThatCode(() -> config.govBrOidcClient(om, props, httpClient, query, event -> {}))
                .doesNotThrowAnyException();
    }

    @Test
    void mockTrueMaisEnvDev_passaSemGuard() {
        GovBrOidcConfiguration config = new GovBrOidcConfiguration();
        GovBrOidcProperties props = propsWithMock(true);
        MockGuardEnvironmentQuery query = queryWith(false, MockGuardProfile.DEV);
        HttpClient httpClient = mock(HttpClient.class);
        ObjectMapper om = new ObjectMapper();

        assertThatCode(() -> config.govBrOidcClient(om, props, httpClient, query, event -> {}))
                .doesNotThrowAnyException();
    }

    @Test
    void mockTrueMaisEnvProd_lancaMockGuardViolationException() {
        GovBrOidcConfiguration config = new GovBrOidcConfiguration();
        GovBrOidcProperties props = propsWithMock(true);
        MockGuardEnvironmentQuery query = queryWith(true, MockGuardProfile.PROD);
        HttpClient httpClient = mock(HttpClient.class);
        ObjectMapper om = new ObjectMapper();

        assertThatThrownBy(() -> config.govBrOidcClient(om, props, httpClient, query, event -> {}))
                .isInstanceOf(MockGuardViolationException.class)
                .hasMessageContaining("pjb.integrations.govbr.mock-enabled");
    }
}
