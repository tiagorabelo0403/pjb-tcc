package com.tcc.pjb.backend.service.oficial_justica;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class OficialJusticaPainelRefinementArchitectureTest {

    private static final Path SERVICE_PATH = Path.of("src/main/java/com/tcc/pjb/backend/service/oficial_justica/OficialJusticaPainelService.java");

    @Test
    void oficialJusticaPainelServiceMustStayBelowServiceHotspotThreshold() throws IOException {
        long lineCount = Files.lines(SERVICE_PATH, StandardCharsets.UTF_8).count();

        assertThat(lineCount).isLessThan(900);
    }

    @Test
    void oficioWorkflowMustRemainExtractedFromPainelService() throws IOException {
        String source = Files.readString(SERVICE_PATH, StandardCharsets.UTF_8);

        assertThat(source).doesNotContain("private WorkItem criarJuntadaDiretaNoProcesso(");
        assertThat(source).doesNotContain("private Map<String, Object> directProcessDispatchTopology(");
        assertThat(source).doesNotContain("private ResolucaoDestinatarioProcessualResult resolverDestinatario(");
        assertThat(source).contains("OficialJusticaOficioWorkflowSupport");
    }
}
