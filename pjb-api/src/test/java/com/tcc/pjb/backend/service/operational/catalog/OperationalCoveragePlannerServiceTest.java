package com.tcc.pjb.backend.service.operational.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.model.entity.secretariat.SecretariatQueueItem;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class OperationalCoveragePlannerServiceTest {

    private final OperationalCoveragePlannerService service = new OperationalCoveragePlannerService(new ObjectMapper());

    @Test
    void shouldProjectSecretariatCoverageWithRedistributionSignals() {
        Instant now = Instant.now();
        SecretariatQueueItem overdueBlocking = SecretariatQueueItem.builder()
                .workItemId(10L)
                .processoId(100L)
                .inboxKey("SECRETARIA:ESTADUAL:TJCE:CE:FORTALEZA:1VARA")
                .queueCode("AUDIENCIA_INTIMACAO")
                .laneCode("AGENDA")
                .deskAxis("CELULA_A")
                .status("PENDENTE")
                .prioridade(1)
                .dueAt(now.minusSeconds(3600))
                .score(95)
                .titulo("Confirmar audiência")
                .hearingSensitive(true)
                .blocking(true)
                .secrecyReviewRequired(false)
                .updatedAt(now)
                .createdAt(now.minusSeconds(7200))
                .rowVersion(1L)
                .build();
        SecretariatQueueItem unassigned = SecretariatQueueItem.builder()
                .workItemId(11L)
                .processoId(101L)
                .inboxKey("SECRETARIA:ESTADUAL:TJCE:CE:FORTALEZA:1VARA")
                .queueCode("EXPEDICAO_MANDADO")
                .laneCode("EXPEDICAO")
                .deskAxis("CELULA_B")
                .status("PENDENTE")
                .prioridade(2)
                .dueAt(now.plusSeconds(7200))
                .score(70)
                .titulo("Expedir mandado")
                .hearingSensitive(false)
                .blocking(false)
                .secrecyReviewRequired(true)
                .updatedAt(now)
                .createdAt(now.minusSeconds(3600))
                .rowVersion(1L)
                .build();

        OperationalCoveragePlannerService.CoverageProjection projection = service.resolveSecretariat(
                "SECRETARIA:ESTADUAL:TJCE:CE:FORTALEZA:1VARA",
                List.of(overdueBlocking, unassigned)
        );

        assertThat(projection.slices()).isNotEmpty();
        assertThat(projection.gaps()).isNotEmpty();
        assertThat(projection.metrics()).containsEntry("unassignedItems", 1L);
        assertThat(projection.metrics()).containsEntry("blockingItems", 1L);
        assertThat(projection.warnings()).isNotEmpty();
    }
}
