package com.tcc.pjb.backend.service.casefile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.kernel.casefile.CaseFileEventStore;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;
import com.tcc.pjb.backend.core.kernel.recursal.RecursalRelationType;
import com.tcc.pjb.backend.model.entity.casefile.CaseContinuityTrack;
import com.tcc.pjb.backend.model.entity.casefile.CaseProceedingRole;
import com.tcc.pjb.backend.model.entity.casefile.CaseProceedingStatus;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.kernel.CaseFileEventEnvelope;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class CaseContinuityObservabilityServiceTest {

    @Test
    void shouldBuildObservabilitySnapshotWithEventDigest() {
        CaseContinuityOrchestratorService orchestratorService = mock(CaseContinuityOrchestratorService.class);
        CaseFileEventStore eventStore = mock(CaseFileEventStore.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        CaseContinuityObservabilityMetrics metrics = mock(CaseContinuityObservabilityMetrics.class);
        CaseContinuityObservabilityService service = new CaseContinuityObservabilityService(orchestratorService, eventStore, auditLedgerService, metrics);

        CaseContinuitySnapshot snapshot = new CaseContinuitySnapshot(
                11L,
                100L,
                100L,
                "ROOT",
                CaseContinuityTrack.EXECUCAO,
                List.of(
                        new CaseContinuityProceedingNode("ROOT", 100L, false, CaseProceedingStatus.ACTIVE, CaseContinuityTrack.CONHECIMENTO, CaseProceedingRole.ROOT, InstanceLevel.FIRST_INSTANCE, "TJCE", "1", null, FaseProcessual.CONHECIMENTO, StatusProcesso.EM_ANDAMENTO, NivelSigilo.PUBLICO, Instant.now()),
                        new CaseContinuityProceedingNode("EXEC", 100L, false, CaseProceedingStatus.ACTIVE, CaseContinuityTrack.EXECUCAO, CaseProceedingRole.EXECUCAO, InstanceLevel.FIRST_INSTANCE, "TJCE", "1", "ROOT", FaseProcessual.EXECUCAO, StatusProcesso.CUMPRIMENTO_SENTENCA, NivelSigilo.PUBLICO, Instant.now().minusSeconds(60 * 60 * 72))
                ),
                List.of(new CaseContinuityEdgeLink("ROOT", "EXEC", RecursalRelationType.EXECUTION_CONTINUATION, LegalAppealType.OUTRO)),
                List.of("atenção")
        );
        when(orchestratorService.inspect(100L)).thenReturn(snapshot);
        when(eventStore.stream(11L)).thenReturn(List.of(
                CaseFileEventEnvelope.builder().caseFileId(11L).seq(1L).eventType("CASE_CONTINUITY_SYNCED").payload("{}").payloadHash("a").createdAt(Instant.now().minusSeconds(120)).build(),
                CaseFileEventEnvelope.builder().caseFileId(11L).seq(2L).eventType("CASE_FILES_MERGED").payload("{}").payloadHash("b").createdAt(Instant.now()).build()
        ));

        var response = service.snapshot(100L);

        assertEquals(2L, response.eventCount());
        assertEquals(1L, response.executoryBranches());
        assertEquals(1L, response.staleProceedings());
        assertTrue(response.attentionRequired());
        assertTrue(response.recentEventTypes().contains("CASE_FILES_MERGED"));
        verify(metrics).recordInspection(any());
        verify(auditLedgerService).appendSafely(eq("CASE_CONTINUITY_OBSERVABILITY_INSPECT"), eq("CASE_FILE"), eq("11"), any(String.class));
    }
}
