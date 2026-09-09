package com.tcc.pjb.backend.ai.common.clients.openai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.tcc.pjb.backend.ai.common.AiProviderException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OpenAiChatCompletionsClientTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private HttpServer server;
    private String baseUrl;
    private volatile String lastRequestBody;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(null);
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    private void stub(int status, String responseBody) {
        server.createContext("/chat/completions", exchange -> {
            lastRequestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            byte[] out = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, out.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(out);
            }
        });
        server.start();
    }

    private OpenAiChatCompletionsClient client() {
        return new OpenAiChatCompletionsClient("chave-de-teste", baseUrl, "gpt-5.2-thinking", 0.2, 512, "v2");
    }

    @Test
    void generateRetornaOConteudoDaPrimeiraEscolha() throws Exception {
        stub(200, MAPPER.writeValueAsString(Map.of(
                "choices", List.of(Map.of("message", Map.of("role", "assistant", "content", "minuta gerada"))))));

        String resultado = client().generate("gere a minuta");

        assertThat(resultado).isEqualTo("minuta gerada");
        Map<?, ?> enviado = MAPPER.readValue(lastRequestBody, Map.class);
        assertThat(enviado.get("model")).isEqualTo("gpt-5.2-thinking");
    }

    @Test
    void erroHttpFalhaComExcecaoTipadaEmVezDeVirarMinuta() {
        stub(429, "{\"error\":{\"message\":\"rate limit exceeded\"}}");

        assertThatThrownBy(() -> client().generate("gere a minuta"))
                .isInstanceOf(AiProviderException.class)
                .hasMessageContaining("openai")
                .hasMessageContaining("HTTP 429");
    }

    @Test
    void mensagemDeFalhaNaoVazaOCorpoDeErroDoProvedor() {
        stub(401, "{\"error\":{\"message\":\"invalid api key sk-abc123\"}}");

        assertThatThrownBy(() -> client().generate("gere a minuta"))
                .isInstanceOf(AiProviderException.class)
                .hasMessageNotContaining("sk-abc123")
                .hasMessageNotContaining("invalid api key");
    }

    @Test
    void respostaSemEscolhasFalhaEmVezDeDevolverMarcadorVazio() throws Exception {
        stub(200, MAPPER.writeValueAsString(Map.of("choices", List.of())));

        assertThatThrownBy(() -> client().generate("gere a minuta"))
                .isInstanceOf(AiProviderException.class)
                .hasMessageContaining("resposta sem conteudo");
    }

    @Test
    void provedorInalcancavelFalhaComExcecaoTipada() {
        OpenAiChatCompletionsClient semServidor =
                new OpenAiChatCompletionsClient("chave", "http://127.0.0.1:1", "modelo", 0.2, 512, "v2");

        assertThatThrownBy(() -> semServidor.generate("gere a minuta"))
                .isInstanceOf(AiProviderException.class);
    }
}
