package com.tcc.pjb.backend.service.innovation;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.dto.innovation.PjbMigrationHygienePreviewRequest;
import org.junit.jupiter.api.Test;

class PjbMigrationHygieneServiceTest {

    private final PjbMigrationHygieneService service = new PjbMigrationHygieneService();

    @Test
    void shouldBlockMigrationWhenCriticalFlagsArePresent() {
        var response = service.preview(new PjbMigrationHygienePreviewRequest(
                "PJe",
                true,
                true,
                false,
                true,
                true,
                true,
                false,
                false,
                false,
                2,
                true
        ));

        assertThat(response.readiness()).isEqualTo("BLOCKED");
        assertThat(response.blockers()).contains("assinaturas pendentes", "audiência agendada", "prazo em aberto", "recurso pendente no tribunal");
        assertThat(response.suggestedJourney()).isEqualTo("TRIBUNAL_COLLEGIATE_SECRETARIAT");
    }

    @Test
    void shouldMarkReadyWhenNoBlockingConditionsExist() {
        var response = service.preview(new PjbMigrationHygienePreviewRequest(
                "eproc",
                false,
                false,
                false,
                false,
                false,
                false,
                true,
                false,
                false,
                0,
                false
        ));

        assertThat(response.readiness()).isEqualTo("READY");
        assertThat(response.blockers()).isEmpty();
        assertThat(response.readinessScore()).isGreaterThanOrEqualTo(90);
    }
}
