package com.tcc.pjb.backend.core.quality.apisurface;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProcessualParticipacaoAtivaRouteGuardTest {

    @Test
    void participacaoAtivaDeveUsarOperationalApiRoutesNosArquivosCriticos() {
        List<Path> files = List.of(
                Path.of("src/main/java/com/tcc/pjb/backend/controller/processual/participacao/workspace/ProcessualParticipacaoWorkspaceController.java"),
                Path.of("src/main/java/com/tcc/pjb/backend/controller/processual/participacao/submission/ProcessualParticipacaoSubmissionController.java"),
                Path.of("src/test/java/com/tcc/pjb/backend/core/quality/apisurface/PjbOperationalRouteCanonicalizationTest.java")
        );

        List<String> missing = files.stream()
                .filter(path -> !ApiSurfaceTestSupport.read(path).contains("OperationalApiRoutes"))
                .map(Path::toString)
                .toList();

        assertTrue(missing.isEmpty(), "Participação ativa ainda fora da rota canônica centralizada: " + missing);
    }
}
