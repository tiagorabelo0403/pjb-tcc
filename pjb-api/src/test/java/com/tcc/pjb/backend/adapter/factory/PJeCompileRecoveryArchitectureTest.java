package com.tcc.pjb.backend.adapter.factory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class PJeCompileRecoveryArchitectureTest {

    private static final Path MAIN = Path.of("src/main/java");

    @Test
    void naoDeveManterLombokNoClusterRecuperadoDoPJe() throws Exception {
        List<String> files = List.of(
                "com/tcc/pjb/backend/model/entity/enums/RamoDireito.java",
                "com/tcc/pjb/backend/shared/dto/PJeAutenticacaoResponse.java",
                "com/tcc/pjb/backend/shared/dto/PJeSubmissaoResponse.java",
                "com/tcc/pjb/backend/shared/dto/PJeAndamentoResponse.java",
                "com/tcc/pjb/backend/adapter/worker/PJeSubmissionWorker.java"
        );
        for (String relative : files) {
            String content = Files.readString(MAIN.resolve(relative));
            assertFalse(content.contains("lombok"), relative);
            assertFalse(content.contains("@Getter"), relative);
            assertFalse(content.contains("@Builder"), relative);
            assertFalse(content.contains("@RequiredArgsConstructor"), relative);
        }
    }

    @Test
    void deveManterWorkerComConstrutorExplicitoParaInjecao() throws Exception {
        String content = Files.readString(MAIN.resolve("com/tcc/pjb/backend/adapter/worker/PJeSubmissionWorker.java"));
        assertTrue(content.contains("public PJeSubmissionWorker(PJeAdapterFactory adapterFactory)"));
    }
}
