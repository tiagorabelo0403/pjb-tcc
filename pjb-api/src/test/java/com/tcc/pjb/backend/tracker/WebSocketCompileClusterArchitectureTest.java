package com.tcc.pjb.backend.tracker;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class WebSocketCompileClusterArchitectureTest {

    private static final Path MAIN = Path.of("src/main/java");

    @Test
    void naoDeveManterLombokNoClusterRecuperadoDeWebsocketEJurisdicao() throws Exception {
        List<String> files = List.of(
                "com/tcc/pjb/backend/tracker/UserActivitySocketHandler.java",
                "com/tcc/pjb/backend/adapter/strategies/config/WebSocketConfig.java",
                "com/tcc/pjb/backend/model/entity/Jurisdicao.java",
                "com/tcc/pjb/backend/model/entity/JurisdictionEngine.java"
        );
        for (String relative : files) {
            String content = Files.readString(MAIN.resolve(relative));
            assertFalse(content.contains("lombok"), relative);
            assertFalse(content.contains("@Getter"), relative);
            assertFalse(content.contains("@Builder"), relative);
            assertFalse(content.contains("@RequiredArgsConstructor"), relative);
            assertFalse(content.contains("@NoArgsConstructor"), relative);
            assertFalse(content.contains("@Setter"), relative);
        }
    }

    @Test
    void deveManterConstrutoresExplicitosNosArquivosRecuperados() throws Exception {
        String handler = Files.readString(MAIN.resolve("com/tcc/pjb/backend/tracker/UserActivitySocketHandler.java"));
        String config = Files.readString(MAIN.resolve("com/tcc/pjb/backend/adapter/strategies/config/WebSocketConfig.java"));
        String jurisdicao = Files.readString(MAIN.resolve("com/tcc/pjb/backend/model/entity/Jurisdicao.java"));
        assertTrue(handler.contains("public UserActivitySocketHandler(KafkaTemplate<String, Object> kafkaTemplate, ObjectMapper objectMapper)"));
        assertTrue(config.contains("public WebSocketConfig(UserActivitySocketHandler socketHandler)"));
        assertTrue(jurisdicao.contains("public Jurisdicao()"));
    }
}
