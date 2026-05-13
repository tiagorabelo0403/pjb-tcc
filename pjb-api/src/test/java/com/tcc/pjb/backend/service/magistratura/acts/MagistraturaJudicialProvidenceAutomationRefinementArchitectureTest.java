package com.tcc.pjb.backend.service.magistratura.acts;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MagistraturaJudicialProvidenceAutomationRefinementArchitectureTest {

    private static final Path SERVICE_PATH = Path.of("src/main/java/com/tcc/pjb/backend/service/magistratura/acts/MagistraturaJudicialProvidenceAutomationService.java");

    @Test
    void automationServiceDeveDelegarPlanejamentoEDispatchParaSupportsExplicitos() throws IOException {
        String source = Files.readString(SERVICE_PATH, StandardCharsets.UTF_8);

        assertThat(source).contains("MagistraturaJudicialProvidencePlanningSupport");
        assertThat(source).contains("MagistraturaJudicialProvidenceDispatchSupport");
        assertThat(source).contains("planningSupport.preview(");
        assertThat(source).contains("dispatchSupport.dispatch(");
        assertThat(source).doesNotContain("private List<ProvidencePlan> buildPlans(");
        assertThat(source).doesNotContain("private WorkItem createWorkItem(");
        assertThat(source).doesNotContain("private DeskTarget resolveDeskTarget(");
    }
}
