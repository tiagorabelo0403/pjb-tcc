package com.tcc.pjb.backend.service.magistratura.acts;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MagistraturaJudicialActWorkbenchRefinementArchitectureTest {

    private static final Path SERVICE_PATH = Path.of("src/main/java/com/tcc/pjb/backend/service/magistratura/acts/MagistraturaJudicialActWorkbenchService.java");

    @Test
    void workbenchServiceMustStayBelowHotspotThresholdAfterDecomposition() throws IOException {
        long lineCount = Files.lines(SERVICE_PATH, StandardCharsets.UTF_8).count();

        assertThat(lineCount).isLessThan(420);
    }

    @Test
    void workbenchServiceMustKeepProjectionAndExecutionSupportsExtracted() throws IOException {
        String source = Files.readString(SERVICE_PATH, StandardCharsets.UTF_8);

        assertThat(source).contains("MagistraturaJudicialActProjectionSupport");
        assertThat(source).contains("MagistraturaJudicialActExecutionSupport");
        assertThat(source).doesNotContain("private Map<String, Object> registrarDespachoRelatoria(");
        assertThat(source).doesNotContain("private Map<String, Object> registrarDecisaoMonocraticaRelatoria(");
        assertThat(source).doesNotContain("private List<MagistraturaJudicialActAvailabilityResponse> catalog(");
        assertThat(source).doesNotContain("private List<MagistraturaJudicialActFieldResponse> fieldsFor(");
        assertThat(source).doesNotContain("private String nativeRoute(");
        assertThat(source).doesNotContain("private String templateFor(");
    }
}
