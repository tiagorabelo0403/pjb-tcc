package com.tcc.pjb.backend.core.quality.codebase;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.core.quality.codebase.application.PjbCodebaseSanityApplicationService;
import com.tcc.pjb.backend.core.quality.codebase.application.PjbCodebaseSanitySettings;
import com.tcc.pjb.backend.core.quality.codebase.domain.PjbCodebaseSanityAggregate;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PjbCodebaseSanityApplicationServiceCacheTest {

    @TempDir
    Path projectRoot;

    @Test
    void deveReutilizarSnapshotEPermitirRefreshForcado() throws IOException {
        prepararEstrutura();
        MutableClock clock = new MutableClock(Instant.parse("2026-04-03T12:00:00Z"));
        PjbCodebaseSanitySettings settings = new PjbCodebaseSanitySettings(
                PjbCodebaseSanitySettings.defaults().allowlistedVirtualThreadFiles(),
                PjbCodebaseSanitySettings.defaults().allowlistedLegacyJudgeImports(),
                Set.of(),
                PjbCodebaseSanitySettings.defaults().blockingIssueCodes(),
                Duration.ofMinutes(10)
        );
        PjbCodebaseSanityApplicationService service = new PjbCodebaseSanityApplicationService(projectRoot, settings, clock);

        PjbCodebaseSanityAggregate initial = service.auditar();
        adicionarMain("src/main/java/com/tcc/pjb/backend/core/processo/painel/PainelB.java", "package com.tcc.pjb.backend.core.processo.painel; class PainelB {}\n");

        PjbCodebaseSanityAggregate cached = service.auditar();
        PjbCodebaseSanityAggregate refreshed = service.auditar(true);

        assertSame(initial, cached);
        assertTrue(refreshed.arquivosEscaneados() > initial.arquivosEscaneados());
    }

    @Test
    void deveExpirarSnapshotAposTtl() throws IOException {
        prepararEstrutura();
        MutableClock clock = new MutableClock(Instant.parse("2026-04-03T12:00:00Z"));
        PjbCodebaseSanitySettings settings = new PjbCodebaseSanitySettings(
                PjbCodebaseSanitySettings.defaults().allowlistedVirtualThreadFiles(),
                PjbCodebaseSanitySettings.defaults().allowlistedLegacyJudgeImports(),
                Set.of(),
                PjbCodebaseSanitySettings.defaults().blockingIssueCodes(),
                Duration.ofMinutes(1)
        );
        PjbCodebaseSanityApplicationService service = new PjbCodebaseSanityApplicationService(projectRoot, settings, clock);

        PjbCodebaseSanityAggregate initial = service.auditar();
        adicionarMain("src/main/java/com/tcc/pjb/backend/core/processo/painel/PainelB.java", "package com.tcc.pjb.backend.core.processo.painel; class PainelB {}\n");
        clock.advance(Duration.ofMinutes(2));

        PjbCodebaseSanityAggregate rebuilt = service.auditar();

        assertTrue(rebuilt.arquivosEscaneados() > initial.arquivosEscaneados());
    }

    private void prepararEstrutura() throws IOException {
        adicionarMain("src/main/java/com/tcc/pjb/backend/core/processo/painel/PainelA.java", "package com.tcc.pjb.backend.core.processo.painel; class PainelA {}\n");
        adicionarPom();
    }

    private void adicionarPom() throws IOException {
        write("pom.xml", "<project><build><plugins><plugin>jacoco-maven-plugin</plugin><plugin>maven-checkstyle-plugin</plugin></plugins></build>config/checkstyle/checkstyle.xml config/checkstyle/bounded-contexts.xml</project>");
    }

    private void adicionarMain(String relative, String content) throws IOException {
        write(relative, content);
    }

    private void write(String relative, String content) throws IOException {
        Path target = projectRoot.resolve(relative);
        if (target.getParent() != null) {
            Files.createDirectories(target.getParent());
        }
        Files.writeString(target, content);
    }

    private static final class MutableClock extends Clock {

        private Instant current;

        private MutableClock(Instant current) {
            this.current = current;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current;
        }

        private void advance(Duration duration) {
            current = current.plus(duration);
        }
    }
}
