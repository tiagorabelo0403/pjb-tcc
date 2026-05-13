package com.tcc.pjb.backend.service.magistratura.acts;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MagistraturaJudicialActExecutionRefinementArchitectureTest {

    private static final Path SERVICE_PATH = Path.of("src/main/java/com/tcc/pjb/backend/service/magistratura/acts/MagistraturaJudicialActExecutionSupport.java");

    @Test
    void executionSupportDeveDelegarParaSupportsDeFormalizacaoERitosColegiados() throws IOException {
        String source = Files.readString(SERVICE_PATH, StandardCharsets.UTF_8);

        assertThat(source).contains("MagistraturaJudicialActRelatoriaFormalizationSupport");
        assertThat(source).contains("MagistraturaJudicialActPanelExecutionSupport");
        assertThat(source).doesNotContain("private Map<String, Object> registrarDespachoRelatoria(");
        assertThat(source).doesNotContain("private Map<String, Object> registrarDecisaoMonocraticaRelatoria(");
        assertThat(source).contains("panelExecutionSupport.executarDecisaoMonocratica(");
        assertThat(source).contains("relatoriaFormalizationSupport.registrarDespachoRelatoria(");
    }
}
