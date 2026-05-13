package com.tcc.pjb.backend.core.quality.apisurface;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ForumHabilitacaoAndJulgamentoRouteGuardTest {

    @Test
    void controladoresCriticosDevemUsarOperationalApiRoutes() {
        List<Path> files = List.of(
                Path.of("src/main/java/com/tcc/pjb/backend/controller/forum/ForumHabilitacaoController.java"),
                Path.of("src/main/java/com/tcc/pjb/backend/controller/secretariat/operational/SecretariatJulgamentoController.java")
        );

        List<String> missing = files.stream()
                .filter(path -> !ApiSurfaceTestSupport.read(path).contains("OperationalApiRoutes"))
                .map(Path::toString)
                .toList();

        assertTrue(missing.isEmpty(), "Fluxos de habilitação e julgamento fora da rota canônica: " + missing);
    }

    @Test
    void naoDeveHaverLiteralDeRotaSensivelEspalhadoNosControladoresCriticos() {
        Map<String, List<Path>> literals = new LinkedHashMap<>();
        literals.put("/api/v1/forum/habilitacoes", List.of(
                Path.of("src/main/java/com/tcc/pjb/backend/controller/forum/ForumHabilitacaoController.java")
        ));
        literals.put("/api/v1/secretariat/julgamentos", List.of(
                Path.of("src/main/java/com/tcc/pjb/backend/controller/secretariat/operational/SecretariatJulgamentoController.java")
        ));

        assertAll(literals.entrySet().stream().map(entry -> () -> {
            List<String> offenders = entry.getValue().stream()
                    .filter(path -> ApiSurfaceTestSupport.read(path).contains(entry.getKey()))
                    .map(Path::toString)
                    .toList();
            assertTrue(offenders.isEmpty(), "Literal de rota crítica ainda espalhado: " + entry.getKey() + " em " + offenders);
        }));
    }
}
