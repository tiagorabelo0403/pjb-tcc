package com.tcc.pjb.backend.service.semantic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OllamaEmbeddingServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private HttpServer server;
    private String baseUrl;

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
        server.createContext("/api/embed", exchange -> {
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
    void embedRetornaVetorNormalizadoAPartirDaRespostaOllama() throws Exception {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("model", "nomic-embed-text");
        response.put("embeddings", List.of(List.of(3.0, 4.0)));
        stub(200, MAPPER.writeValueAsString(response));

        OllamaEmbeddingService service = new OllamaEmbeddingService(baseUrl, "nomic-embed-text");
        EmbeddingVector vector = service.embed("peticao inicial de cobranca");

        assertThat(vector.values()).hasSize(2);
        assertThat(vector.values()[0]).isCloseTo(0.6f, org.assertj.core.data.Offset.offset(0.001f));
        assertThat(vector.values()[1]).isCloseTo(0.8f, org.assertj.core.data.Offset.offset(0.001f));
    }

    @Test
    void embedLancaExcecaoEmErroHttp() {
        stub(500, "{\"error\":\"model not found\"}");

        OllamaEmbeddingService service = new OllamaEmbeddingService(baseUrl, "nomic-embed-text");

        assertThatThrownBy(() -> service.embed("texto"))
                .isInstanceOf(OllamaEmbeddingService.OllamaEmbeddingException.class)
                .hasMessageContaining("status=500");
    }
}
