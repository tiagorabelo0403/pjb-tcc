package com.tcc.pjb.backend.core.quality.apisurface;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OfficialOperationalRouteLiteralGuardTest {

    @Test
    void officialFlowArquivosCriticosDevemUsarOperationalApiRoutes() {
        List<Path> files = List.of(
                Path.of("src/main/java/com/tcc/pjb/backend/controller/forum/ForumOfficialReturnController.java"),
                Path.of("src/main/java/com/tcc/pjb/backend/controller/juiz/JuizGabineteDecisionalController.java"),
                Path.of("src/main/java/com/tcc/pjb/backend/service/forum/ForumOfficialReturnOperationalService.java"),
                Path.of("src/main/java/com/tcc/pjb/backend/service/juiz/decision/GabineteOficialRetornoTriagemService.java"),
                Path.of("src/main/java/com/tcc/pjb/backend/service/secretariat/oficial/SecretariaOficialCumprimentoRoutingService.java"),
                Path.of("src/main/java/com/tcc/pjb/backend/service/secretariat/oficial/SecretariatOfficialActsDrawerService.java")
        );

        List<String> missing = files.stream()
                .filter(path -> !ApiSurfaceTestSupport.read(path).contains("OperationalApiRoutes"))
                .map(Path::toString)
                .toList();

        assertTrue(missing.isEmpty(), "Fluxo crítico do Oficial ainda fora da rota canônica centralizada: " + missing);
    }

    @Test
    void officialFlowArquivosCriticosNaoDevemConterLiteraisDeRotasSensivesDuplicadas() {
        Map<String, List<Path>> literals = new LinkedHashMap<>();
        literals.put("/api/v1/forum/oficial-retornos", List.of(
                Path.of("src/main/java/com/tcc/pjb/backend/controller/forum/ForumOfficialReturnController.java"),
                Path.of("src/main/java/com/tcc/pjb/backend/service/forum/ForumOfficialReturnOperationalService.java")
        ));
        literals.put("/api/v1/secretariat/operacional/oficial-cumprimentos", List.of(
                Path.of("src/main/java/com/tcc/pjb/backend/service/secretariat/oficial/SecretariaOficialCumprimentoRoutingService.java"),
                Path.of("src/main/java/com/tcc/pjb/backend/service/secretariat/oficial/SecretariatOfficialActsDrawerService.java")
        ));
        literals.put("/api/v1/juiz/gabinete-decisoes/oficial-retornos", List.of(
                Path.of("src/main/java/com/tcc/pjb/backend/controller/juiz/JuizGabineteDecisionalController.java"),
                Path.of("src/main/java/com/tcc/pjb/backend/service/juiz/decision/GabineteOficialRetornoTriagemService.java")
        ));

        for (Map.Entry<String, List<Path>> entry : literals.entrySet()) {
            List<String> offenders = entry.getValue().stream()
                    .filter(path -> ApiSurfaceTestSupport.read(path).contains(entry.getKey()))
                    .map(Path::toString)
                    .toList();
            assertTrue(offenders.isEmpty(), "Literal de rota operacional sensível ainda espalhado fora de OperationalApiRoutes: " + entry.getKey() + " em " + offenders);
        }
    }
}
