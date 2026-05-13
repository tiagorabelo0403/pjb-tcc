package com.tcc.pjb.backend.governance.layout;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class CriticalEnumComparisonGovernanceTest {

    private static final Pattern ENUM_NAME_LITERAL = Pattern.compile("\\.name\\(\\)\\.equals(?:IgnoreCase)?\\(\\s*\"");
    private static final Pattern LITERAL_EQUALS_ENUM_NAME = Pattern.compile("\"[A-Z0-9_]+\"\\.equals(?:IgnoreCase)?\\([^;\\n]*\\.name\\(\\)");
    private static final List<Path> CRITICAL_FILES = List.of(
            Path.of("src/main/java/com/tcc/pjb/backend/service/workitem/WorkItemService.java"),
            Path.of("src/main/java/com/tcc/pjb/backend/service/judge/JudgeDocketService.java"),
            Path.of("src/main/java/com/tcc/pjb/backend/service/processo/ProcessoPostAjuizamentoOrchestratorService.java"),
            Path.of("src/main/java/com/tcc/pjb/backend/service/ajuizamento/federal/FederalismoRedistribuicaoService.java"),
            Path.of("src/main/java/com/tcc/pjb/backend/core/comunicacao/institucional/entry/application/InstitutionalEntryContextApplicationService.java"),
            Path.of("src/main/java/com/tcc/pjb/backend/modules/laiane/service/LaianeLawyerService.java"),
            Path.of("src/test/java/com/tcc/pjb/backend/core/comunicacao/institucional/access/VinculoUsuarioCaixaInstitucionalResolverTest.java")
    );

    @Test
    void arquivosCriticosNaoDevemCompararEnumsPorNameMaisStringLiteral() throws IOException {
        for (Path path : CRITICAL_FILES) {
            String source = Files.readString(path);
            assertFalse(ENUM_NAME_LITERAL.matcher(source).find(), () -> "Comparação enum-string por name() em: " + path);
            assertFalse(LITERAL_EQUALS_ENUM_NAME.matcher(source).find(), () -> "Comparação string-enum por name() em: " + path);
        }
    }
}
