package com.tcc.pjb.backend.core.peticionamento.saga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.peticionamento.saga.domain.ProtocoloSagaPeticionamentoResult;
import com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaExecutionStep;
import com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaExecutionTimeline;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class PeticionamentoSagaApplicationServiceTest {

    @Test
    void gerarProtocolo_deveAuditarNumero() {
        PeticionamentoSagaOrchestrator orchestrator = mock(PeticionamentoSagaOrchestrator.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        when(orchestrator.gerarProtocolo(new com.tcc.pjb.backend.core.peticionamento.saga.domain.GerarProtocoloSagaCommand(8L)))
                .thenReturn(new ProtocoloSagaPeticionamentoResult(8L, "P-2026-1", Instant.parse("2026-04-11T12:00:00Z"), "connector"));
        PeticionamentoSagaApplicationService applicationService = new PeticionamentoSagaApplicationService(orchestrator, auditLedgerService);

        var result = applicationService.gerarProtocolo(8L);

        assertThat(result.numeroProtocolo()).isEqualTo("P-2026-1");
        verify(auditLedgerService).appendSafely(eq("SAGA_PROTOCOLO_MANUAL"), eq("PETICAO"), eq("8"), isNull(), eq("P-2026-1"));
    }

    @Test
    void timeline_deveAuditarEtapas() {
        PeticionamentoSagaOrchestrator orchestrator = mock(PeticionamentoSagaOrchestrator.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        when(orchestrator.executionTimeline(8L)).thenReturn(new SagaExecutionTimeline(8L, List.of(
                new SagaExecutionStep("VALIDAR", true, Instant.parse("2026-04-11T12:00:00Z")),
                new SagaExecutionStep("PROTOCOLO", true, Instant.parse("2026-04-11T12:05:00Z"))
        )));
        PeticionamentoSagaApplicationService applicationService = new PeticionamentoSagaApplicationService(orchestrator, auditLedgerService);

        var result = applicationService.timeline(8L);

        assertThat(result.steps()).hasSize(2);
        verify(auditLedgerService).appendSafely(eq("SAGA_TIMELINE_QUERY"), eq("PETICAO"), eq("8"), isNull(), eq("steps=2"));
    }
}
