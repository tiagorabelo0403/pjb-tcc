package com.tcc.pjb.backend.governance.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import com.tcc.pjb.backend.testsupport.PjbTestPaths;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class LegacyJudgePackageIsolationGovernanceTest {

    private static final Path ROOT = PjbTestPaths.projectRoot();
    private static final Path MAIN_JAVA = PjbTestPaths.pjbApiMainJavaRoot();
    private static final String LEGACY_JUDGE_IMPORT = "import com.tcc.pjb.backend.service.judge.";
    private static final String LEGACY_JUDGE_PACKAGE_SEGMENT = "/service/judge/";
    private static final String CANONICAL_LEGACY_BRIDGE = "src/main/java/com/tcc/pjb/backend/controller/JudgeDocketController.java";
    private static final String CANONICAL_LEGACY_SERVICE = "src/main/java/com/tcc/pjb/backend/service/judge/JudgeDocketService.java";

    @Test
    void legacyJudgePackageDevePermanecerComoPonteIsolada() throws IOException {
        try (Stream<Path> stream = Files.walk(MAIN_JAVA)) {
            List<String> legacyFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .map(LegacyJudgePackageIsolationGovernanceTest::relativeMainJavaPath)
                    .filter(path -> path.contains(LEGACY_JUDGE_PACKAGE_SEGMENT))
                    .sorted()
                    .toList();
            assertEquals(List.of(CANONICAL_LEGACY_SERVICE), legacyFiles);
        }
    }

    @Test
    void somenteORoteadorCanonicoPodeImportarLegacyJudgeService() throws IOException {
        try (Stream<Path> stream = Files.walk(MAIN_JAVA)) {
            List<String> imports = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(this::containsLegacyJudgeImport)
                    .map(LegacyJudgePackageIsolationGovernanceTest::relativeMainJavaPath)
                    .sorted()
                    .toList();
            assertEquals(List.of(CANONICAL_LEGACY_BRIDGE), imports);
        }
    }

    @Test
    void adrDeveFormalizarQueJudgeEhSomenteLegadoTransitorio() throws IOException {
        Path adr = ROOT.resolve("docs/adr/ADR-0002-isolamento-do-legado-judge-e-gates-de-qualidade.md");
        assertTrue(Files.isRegularFile(adr));
        String content = Files.readString(adr, StandardCharsets.UTF_8);
        assertTrue(content.contains("judge"));
        assertTrue(content.contains("juiz"));
        assertTrue(content.contains("JaCoCo"));
        assertTrue(content.contains("Checkstyle"));
    }

    private static String relativeMainJavaPath(Path path) {
        return "src/main/java/" + MAIN_JAVA.relativize(path).toString().replace('\\', '/');
    }

    private boolean containsLegacyJudgeImport(Path path) {
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            return content.contains(LEGACY_JUDGE_IMPORT);
        } catch (IOException ex) {
            return false;
        }
    }
}
