package com.tcc.pjb.backend.governance.source;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class OperationalRouteConstantUniquenessGovernanceTest {

    @Test
    void naoDeveHaverConstantesDeRotasOperacionaisComMesmoValorLiteral() {
        List<String> offenders = SourceGovernanceScanner.duplicateOperationalRouteConstants();
        assertTrue(offenders.isEmpty(), "Constantes duplicadas em OperationalApiRoutes: " + offenders);
    }

    @Test
    void participacaoAtivaNaoDeveEspalharLiteraisCriticosForaDaClasseCanonica() {
        List<Path> files = List.of(
                Path.of("src/main/java/com/tcc/pjb/backend/controller/processual/participacao/workspace/ProcessualParticipacaoWorkspaceController.java"),
                Path.of("src/main/java/com/tcc/pjb/backend/controller/processual/participacao/submission/ProcessualParticipacaoSubmissionController.java"),
                Path.of("src/main/java/com/tcc/pjb/backend/controller/processual/participacao/support/ProcessualParticipacaoControllerRateLimitSupport.java"),
                Path.of("src/main/java/com/tcc/pjb/backend/service/processual/participacao/ProcessualParticipacaoAtivaFacadeService.java"),
                Path.of("src/test/java/com/tcc/pjb/backend/core/quality/apisurface/PjbOperationalRouteCanonicalizationTest.java")
        );

        List<String> literals = List.of(
                "/api/v1/processual/processos",
                "/participacao-ativa/workspace",
                "/participacao-ativa/protocolar",
                "/participacao-ativa/submissoes"
        );

        for (String literal : literals) {
            List<String> offenders = files.stream()
                    .filter(Files::exists)
                    .filter(path -> containsLiteral(path, literal))
                    .map(Path::toString)
                    .toList();
            assertTrue(offenders.isEmpty(), "Literal de rota de participação ativa ainda espalhado fora de OperationalApiRoutes: " + literal + " em " + offenders);
        }
    }

    private boolean containsLiteral(Path path, String literal) {
        try {
            return Files.readString(path).contains(literal);
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao ler arquivo: " + path, ex);
        }
    }
}
