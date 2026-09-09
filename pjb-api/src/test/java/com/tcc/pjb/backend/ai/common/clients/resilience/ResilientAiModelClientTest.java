package com.tcc.pjb.backend.ai.common.clients.resilience;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.junit.jupiter.api.Test;
import com.tcc.pjb.backend.ai.common.AiModelClient;
import com.tcc.pjb.backend.ai.common.AiProviderException;

class ResilientAiModelClientTest {

    private static final class ProvedorInstavel implements AiModelClient {
        private int chamadas;
        private boolean falhar = true;
        private long timeoutRecebido;

        @Override
        public String generate(String prompt) {
            chamadas++;
            if (falhar) {
                throw new AiProviderException("openai", "HTTP 503");
            }
            return "resposta";
        }

        @Override
        public void setTimeout(long millis) {
            this.timeoutRecebido = millis;
        }
    }

    private static CircuitBreaker breakerQueAbreApos(int chamadas) {
        return CircuitBreaker.of("ai-model-teste", CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(chamadas)
                .minimumNumberOfCalls(chamadas)
                .failureRateThreshold(50f)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .build());
    }

    @Test
    void repassaARespostaQuandoOProvedorEstaSaudavel() {
        ProvedorInstavel provedor = new ProvedorInstavel();
        provedor.falhar = false;
        ResilientAiModelClient client =
                new ResilientAiModelClient(provedor, breakerQueAbreApos(4), "openai");

        assertThat(client.generate("gere a minuta")).isEqualTo("resposta");
        assertThat(provedor.chamadas).isEqualTo(1);
    }

    @Test
    void aposFalhasConsecutivasOCircuitoAbreEDeixaDeChamarOProvedor() {
        ProvedorInstavel provedor = new ProvedorInstavel();
        ResilientAiModelClient client =
                new ResilientAiModelClient(provedor, breakerQueAbreApos(4), "openai");

        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> client.generate("gere a minuta"))
                    .isInstanceOf(AiProviderException.class);
        }
        int chamadasAteAbrir = provedor.chamadas;

        assertThatThrownBy(() -> client.generate("gere a minuta"))
                .isInstanceOf(AiProviderException.class)
                .hasMessageContaining("circuito aberto");

        assertThat(provedor.chamadas)
                .as("com o circuito aberto o provedor nao pode ser chamado de novo")
                .isEqualTo(chamadasAteAbrir);
    }

    @Test
    void falhaComCircuitoAbertoUsaOMesmoTipoDeExcecaoDasDemaisFalhasDeProvedor() {
        ProvedorInstavel provedor = new ProvedorInstavel();
        ResilientAiModelClient client =
                new ResilientAiModelClient(provedor, breakerQueAbreApos(2), "ollama");

        for (int i = 0; i < 2; i++) {
            assertThatThrownBy(() -> client.generate("x")).isInstanceOf(AiProviderException.class);
        }

        assertThatThrownBy(() -> client.generate("x"))
                .isInstanceOf(AiProviderException.class)
                .hasMessageContaining("ollama");
    }

    @Test
    void geracaoPorContextoTambemPassaPeloCircuito() {
        ProvedorInstavel provedor = new ProvedorInstavel();
        ResilientAiModelClient client =
                new ResilientAiModelClient(provedor, breakerQueAbreApos(2), "openai");

        for (int i = 0; i < 2; i++) {
            assertThatThrownBy(() -> client.generate("x")).isInstanceOf(AiProviderException.class);
        }
        int chamadasAteAbrir = provedor.chamadas;

        assertThatThrownBy(() -> client.generate(java.util.Map.of("peticao", "texto")))
                .isInstanceOf(AiProviderException.class);
        assertThat(provedor.chamadas).isEqualTo(chamadasAteAbrir);
    }

    @Test
    void configuracaoDeTimeoutChegaAoProvedorReal() {
        ProvedorInstavel provedor = new ProvedorInstavel();
        ResilientAiModelClient client =
                new ResilientAiModelClient(provedor, breakerQueAbreApos(2), "openai");

        client.setTimeout(4321L);

        assertThat(provedor.timeoutRecebido).isEqualTo(4321L);
        assertThat(client.delegate()).isSameAs(provedor);
    }
}
