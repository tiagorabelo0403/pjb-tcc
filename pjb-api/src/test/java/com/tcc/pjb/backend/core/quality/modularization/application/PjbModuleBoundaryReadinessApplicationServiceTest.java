package com.tcc.pjb.backend.core.quality.modularization.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PjbModuleBoundaryReadinessApplicationServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void snapshot_deveApontarBloqueadoresDeExtracaoInicial() throws Exception {
        Files.createDirectories(tempDir.resolve("src/main/java/com/tcc/pjb/backend/core/demo"));
        Files.createDirectories(tempDir.resolve("src/main/java/com/tcc/pjb/backend/controller/demo"));
        Files.writeString(tempDir.resolve("pom.xml"), "<project><packaging>jar</packaging></project>", StandardCharsets.UTF_8);
        Files.writeString(
                tempDir.resolve("src/main/java/com/tcc/pjb/backend/core/demo/CoreLeak.java"),
                "package com.tcc.pjb.backend.core.demo;\n" +
                        "import com.tcc.pjb.backend.service.foo.LegacyService;\n" +
                        "public class CoreLeak {}\n",
                StandardCharsets.UTF_8);
        Files.writeString(
                tempDir.resolve("src/main/java/com/tcc/pjb/backend/controller/demo/LeakController.java"),
                "package com.tcc.pjb.backend.controller.demo;\n" +
                        "import com.tcc.pjb.backend.model.repository.ProcessoRepository;\n" +
                        "public class LeakController {}\n",
                StandardCharsets.UTF_8);

        PjbModuleBoundaryReadinessApplicationService service = new PjbModuleBoundaryReadinessApplicationService(tempDir);

        var snapshot = service.snapshot();

        assertThat(snapshot.aggregatorPomPresent()).isFalse();
        assertThat(snapshot.coreExtractionReady()).isFalse();
        assertThat(snapshot.blockers()).extracting("code").contains("core.depends.on.service", "controller.depends.on.repository", "root.without.modules");
    }

    @Test
    void packages_deveAgruparTopologiasPorModuloAlvo() throws Exception {
        Files.createDirectories(tempDir.resolve("src/main/java/com/tcc/pjb/backend/core/prazos"));
        Files.createDirectories(tempDir.resolve("src/main/java/com/tcc/pjb/backend/controller/admin"));
        Files.createDirectories(tempDir.resolve("src/main/java/com/tcc/pjb/backend/integration/mni"));
        Files.writeString(tempDir.resolve("pom.xml"), "<project><modules><module>pjb-core</module></modules></project>", StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("src/main/java/com/tcc/pjb/backend/core/prazos/Prazo.java"), "package com.tcc.pjb.backend.core.prazos; public class Prazo {}", StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("src/main/java/com/tcc/pjb/backend/controller/admin/AdminDemoController.java"), "package com.tcc.pjb.backend.controller.admin; public class AdminDemoController {}", StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("src/main/java/com/tcc/pjb/backend/integration/mni/MniAdapter.java"), "package com.tcc.pjb.backend.integration.mni; public class MniAdapter {}", StandardCharsets.UTF_8);

        PjbModuleBoundaryReadinessApplicationService service = new PjbModuleBoundaryReadinessApplicationService(tempDir);

        var packages = service.packages();

        assertThat(packages).extracting("moduleName").contains("pjb-core", "pjb-api", "pjb-integration");
        assertThat(packages.stream().filter(item -> item.moduleName().equals("pjb-core")).findFirst()).isPresent();
    }
}
