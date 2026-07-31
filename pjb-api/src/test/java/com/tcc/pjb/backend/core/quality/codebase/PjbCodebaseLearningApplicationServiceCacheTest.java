package com.tcc.pjb.backend.core.quality.codebase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.core.quality.codebase.application.PjbCodebaseLearningApplicationService;
import com.tcc.pjb.backend.core.quality.codebase.application.PjbCodebaseLearningSettings;
import com.tcc.pjb.backend.core.quality.codebase.domain.PjbCodebaseLearningAggregate;
import com.tcc.pjb.backend.support.MutableClock;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PjbCodebaseLearningApplicationServiceCacheTest {

    @TempDir
    Path projectRoot;

    @Test
    void deveReutilizarSnapshotDentroDaJanelaDeCacheEPermitirRefreshForcado() throws IOException {
        prepararEstrutura();
        MutableClock clock = new MutableClock(Instant.parse("2026-04-03T12:00:00Z"));
        PjbCodebaseLearningSettings settings = new PjbCodebaseLearningSettings(
                "com.tcc.pjb.backend.",
                0.20d,
                0.30d,
                0.10d,
                6,
                Duration.ofMinutes(10),
                PjbCodebaseLearningSettings.defaults().criticalFlows()
        );
        PjbCodebaseLearningApplicationService service = new PjbCodebaseLearningApplicationService(projectRoot, settings, clock);

        PjbCodebaseLearningAggregate initial = service.aprender();
        adicionarMain("src/main/java/com/tcc/pjb/backend/core/processo/painel/PainelB.java", "package com.tcc.pjb.backend.core.processo.painel; class PainelB {}\n");

        PjbCodebaseLearningAggregate cached = service.aprender();
        PjbCodebaseLearningAggregate refreshed = service.aprender(true);

        assertSame(initial, cached);
        assertEquals(initial.arquivosMain(), cached.arquivosMain());
        assertTrue(refreshed.arquivosMain() > initial.arquivosMain());
    }

    @Test
    void deveExpirarSnapshotAoUltrapassarTtl() throws IOException {
        prepararEstrutura();
        MutableClock clock = new MutableClock(Instant.parse("2026-04-03T12:00:00Z"));
        PjbCodebaseLearningSettings settings = new PjbCodebaseLearningSettings(
                "com.tcc.pjb.backend.",
                0.20d,
                0.30d,
                0.10d,
                6,
                Duration.ofMinutes(1),
                PjbCodebaseLearningSettings.defaults().criticalFlows()
        );
        PjbCodebaseLearningApplicationService service = new PjbCodebaseLearningApplicationService(projectRoot, settings, clock);

        PjbCodebaseLearningAggregate initial = service.aprender();
        adicionarMain("src/main/java/com/tcc/pjb/backend/core/processo/painel/PainelB.java", "package com.tcc.pjb.backend.core.processo.painel; class PainelB {}\n");
        clock.advance(Duration.ofMinutes(2));

        PjbCodebaseLearningAggregate rebuilt = service.aprender();

        assertTrue(rebuilt.arquivosMain() > initial.arquivosMain());
    }

    private void prepararEstrutura() throws IOException {
        adicionarMain("src/main/java/com/tcc/pjb/backend/core/processo/painel/PainelA.java", "package com.tcc.pjb.backend.core.processo.painel; class PainelA {}\n");
        adicionarTeste("src/test/java/com/tcc/pjb/backend/core/processo/painel/PainelAIT.java", "package com.tcc.pjb.backend.core.processo.painel; class PainelAIT {}\n");
    }

    private void adicionarMain(String relative, String content) throws IOException {
        write(relative, content);
    }

    private void adicionarTeste(String relative, String content) throws IOException {
        write(relative, content);
    }

    private void write(String relative, String content) throws IOException {
        Path target = projectRoot.resolve(relative);
        Files.createDirectories(target.getParent());
        Files.writeString(target, content);
    }
}
