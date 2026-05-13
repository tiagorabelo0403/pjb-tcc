package com.tcc.pjb.backend.service.recursal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import com.tcc.pjb.backend.core.kernel.recursal.LegalIntegrationSystem;
import com.tcc.pjb.backend.core.kernel.recursal.RecursalFactType;
import com.tcc.pjb.backend.core.kernel.recursal.model.CanonicalFact;
import com.tcc.pjb.backend.core.kernel.recursal.model.MovementRecordedPayload;
import com.tcc.pjb.backend.core.kernel.recursal.plan.RecursalPlan;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.RecursalGraphResponse;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;

class RecursalIntelligenceFacadeServiceTest {

    @Test
    void devePreservarDedupKeyMesmoComSigiloNaoPublico() {
        RecursalGraphIngestionService ingestionService = mock(RecursalGraphIngestionService.class);
        RecursalGraphQueryService queryService = mock(RecursalGraphQueryService.class);
        RecursalTimelineIntegrationService timelineIntegration = mock(RecursalTimelineIntegrationService.class);
        RecursalWorkItemMaterializerService workItemMaterializer = mock(RecursalWorkItemMaterializerService.class);
        RecursalOutboxEmitterService outboxEmitter = mock(RecursalOutboxEmitterService.class);
        RecursalEffectiveSecrecyService secrecyService = mock(RecursalEffectiveSecrecyService.class);

        RecursalIntelligenceFacadeService service = new RecursalIntelligenceFacadeService(
                ingestionService,
                queryService,
                timelineIntegration,
                workItemMaterializer,
                outboxEmitter,
                secrecyService
        );

        CanonicalFact fact = new CanonicalFact(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                RecursalFactType.MOVEMENT_RECORDED,
                LegalIntegrationSystem.PJE,
                "dedup-original",
                "PROC-1",
                new MovementRecordedPayload("123", "movimento", "PJE"),
                Instant.parse("2026-04-04T12:00:00Z")
        );
        RecursalPlan plan = new RecursalPlan(List.of(), List.of(), List.of(), List.of(), List.of());
        RecursalGraphResponse graph = new RecursalGraphResponse(99L, "anchor", new RecursalGraphResponse.SummaryDto(0, 0, 0, 0, 0, null), List.of(), List.of());
        when(secrecyService.effectiveSecrecySnapshot(99L)).thenReturn(new RecursalEffectiveSecrecyService.SecrecySnapshot(NivelSigilo.PUBLICO, NivelSigilo.SEGREDO_JUSTICA, 77L, 1, true, "SIGILOSO"));
        when(ingestionService.ingest(99L, fact)).thenReturn(plan);
        when(timelineIntegration.appendTimelineEntry(99L, fact, plan)).thenReturn(501L);
        when(queryService.readGraph(99L)).thenReturn(graph);

        var response = service.ingest(99L, fact);
        assertThat(response.dedupKey()).isEqualTo("dedup-original");
    }
}
