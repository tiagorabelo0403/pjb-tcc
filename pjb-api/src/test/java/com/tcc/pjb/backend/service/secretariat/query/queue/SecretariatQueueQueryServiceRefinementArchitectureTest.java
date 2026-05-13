package com.tcc.pjb.backend.service.secretariat.query.queue;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SecretariatQueueQueryServiceRefinementArchitectureTest {

    private static final Path SERVICE_PATH = Path.of("src/main/java/com/tcc/pjb/backend/service/secretariat/query/queue/SecretariatQueueQueryService.java");

    @Test
    void secretariatQueueQueryServiceMustStayBelowServiceHotspotThreshold() throws IOException {
        long lineCount = Files.lines(SERVICE_PATH, StandardCharsets.UTF_8).count();

        assertThat(lineCount).isLessThan(900);
    }

    @Test
    void queryServiceMustKeepRowPanelAndAgendaSupportsExtracted() throws IOException {
        String source = Files.readString(SERVICE_PATH, StandardCharsets.UTF_8);

        assertThat(source).contains("SecretariatQueuePanelRowProjectionSupport");
        assertThat(source).contains("SecretariatQueuePanelProjectionSupport");
        assertThat(source).contains("SecretariatQueueAgendaProjectionSupport");
        assertThat(source).doesNotContain("private SecretariatQueuePanelRow toSecretariatQueuePanelRow(");
        assertThat(source).doesNotContain("private SecretariatQueuePanelSnapshotDto panel(");
        assertThat(source).doesNotContain("private List<SecretariatQueueAgendaGroupDto> groupAgendaRows(");
    }
}
