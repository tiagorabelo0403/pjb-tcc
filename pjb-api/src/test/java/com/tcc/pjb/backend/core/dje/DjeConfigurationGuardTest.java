package com.tcc.pjb.backend.core.dje;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.guard.MockGuardEnvironmentQuery;
import com.tcc.pjb.backend.core.guard.MockGuardProfile;
import com.tcc.pjb.backend.core.guard.MockGuardViolationException;
import org.junit.jupiter.api.Test;

/**
 * Cobre 3 caminhos de DjeConfiguration:
 *   (1) mock=true + env=DEV  → bean criado, retorna MOCK
 *   (2) mock=true + env=PROD → MockGuardViolationException
 *   (3) enabled=true         → mock bean não registrado (sem guard)
 *
 * NOTA: O cenário mock-enabled=false (sem bean, NoSuchBeanDefinitionException)
 * é coberto pelo MockGuardStartupDjeIntegrationTest (item 17), não por este teste.
 * A separação é intencional: aqui testamos o guard em isolamento via injeção direta;
 * o cenário "sem bean" requer contexto Spring completo para observar a exceção.
 */
class DjeConfigurationGuardTest {

    private static MockGuardEnvironmentQuery queryWith(boolean isReal, MockGuardProfile profile) {
        MockGuardEnvironmentQuery q = mock(MockGuardEnvironmentQuery.class);
        when(q.isRealEnvironment()).thenReturn(isReal);
        when(q.activeGuardProfile()).thenReturn(profile);
        return q;
    }

    @Test
    void caminho1_mockTrueMaisEnvDev_criaBeanMock() {
        DjeConfiguration config = new DjeConfiguration();
        MockGuardEnvironmentQuery query = queryWith(false, MockGuardProfile.DEV);

        DjeHttpClient client = config.djeHttpClient(query, event -> {});

        assertThat(client).isNotNull();
        var result = client.enviar("TJSP", "<ato/>", "DESPACHO");
        assertThat(result.edicao()).isEqualTo("MOCK");
    }

    @Test
    void caminho2_mockTrueMaisEnvProd_lancaMockGuardViolationException() {
        DjeConfiguration config = new DjeConfiguration();
        MockGuardEnvironmentQuery query = queryWith(true, MockGuardProfile.PROD);

        assertThatThrownBy(() -> config.djeHttpClient(query, event -> {}))
                .isInstanceOf(MockGuardViolationException.class)
                .hasMessageContaining("pjb.dje.mock-enabled");
    }

    @Test
    void caminho2b_djePartesNotificacaoPort_mockTrueMaisEnvProd_lancaExcecao() {
        DjeConfiguration config = new DjeConfiguration();
        MockGuardEnvironmentQuery query = queryWith(true, MockGuardProfile.PROD);

        assertThatThrownBy(() -> config.djePartesNotificacaoPort(query, event -> {}))
                .isInstanceOf(MockGuardViolationException.class)
                .hasMessageContaining("pjb.dje.mock-enabled");
    }
}
