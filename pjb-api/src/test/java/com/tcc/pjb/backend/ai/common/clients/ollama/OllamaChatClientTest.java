package com.tcc.pjb.backend.ai.common.clients.ollama;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OllamaChatClientTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private HttpServer server;
    private String baseUrl;
    private volatile String lastRequestBody;
    private volatile String lastRequestPath;

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
        server.createContext("/api/generate", exchange -> {
            lastRequestPath = exchange.getRequestURI().getPath();
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

    @Test
    void generateRetornaOTextoDaRespostaOllama() throws Exception {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("model", "qwen2.5:7b");
        response.put("response", "resposta gerada pelo modelo");
        response.put("done", true);
        stub(200, MAPPER.writeValueAsString(response));

        OllamaChatClient client = new OllamaChatClient(baseUrl, "qwen2.5:7b", 0.2);
        String result = client.generate("qual a capital do ceara");

        assertThat(result).isEqualTo("resposta gerada pelo modelo");
        assertThat(lastRequestPath).isEqualTo("/api/generate");
        Map<?, ?> sentPayload = MAPPER.readValue(lastRequestBody, Map.class);
        assertThat(sentPayload.get("model")).isEqualTo("qwen2.5:7b");
        assertThat(sentPayload.get("prompt")).isEqualTo("qual a capital do ceara");
        assertThat(sentPayload.get("stream")).isEqualTo(false);
    }

    @Test
    void generatePropagaErroHttpComoTextoMarcado() {
        stub(500, "{\"error\":\"model not found\"}");

        OllamaChatClient client = new OllamaChatClient(baseUrl, "qwen2.5:7b", 0.2);
        String result = client.generate("teste");

        assertThat(result).startsWith("[OLLAMA-ERROR] status=500");
    }
}
