package com.tcc.pjb.backend.core.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.observability.domain.PjbSloExecutionQuery;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class PjbSloApplicationServiceTest {

    @Test
    void evaluate_deveMarcarViolacaoQuandoTempoPassaDoBudget() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        PjbSloRegistry sloRegistry = new PjbSloRegistry(meterRegistry);
        sloRegistry.registerSlos();
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        PjbSloApplicationService applicationService = new PjbSloApplicationService(sloRegistry, auditLedgerService);

        Timer.Sample sample = Timer.start(meterRegistry);
        sample.stop(sloRegistry.timer("peticionamento"));

        var result = applicationService.evaluate(new PjbSloExecutionQuery("peticionamento", 4.2));

        assertThat(result.operation()).isEqualTo("peticionamento");
        assertThat(result.sloSeconds()).isEqualTo(3.0);
        assertThat(result.violated()).isTrue();
        verify(auditLedgerService).appendSafely(anyString(), anyString(), anyString(), isNull(), anyString());
    }

    @Test
    void budgetHealth_deveIndicarOperacaoRegistrada() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        PjbSloRegistry sloRegistry = new PjbSloRegistry(meterRegistry);
        sloRegistry.registerSlos();
        PjbSloApplicationService applicationService = new PjbSloApplicationService(sloRegistry, mock(AuditLedgerService.class));

        var view = applicationService.budgetHealth("mni_remessa");

        assertThat(view.operation()).isEqualTo("mni_remessa");
        assertThat(view.healthy()).isTrue();
        assertThat(view.sloSeconds()).isEqualTo(10.0);
    }

    @Test
    void timeline_deveNormalizarEventoEAuditarConsulta() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        PjbSloRegistry sloRegistry = new PjbSloRegistry(meterRegistry);
        sloRegistry.registerSlos();
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        PjbSloApplicationService applicationService = new PjbSloApplicationService(sloRegistry, auditLedgerService);

        var result = applicationService.timeline("peticionamento", "Violation Detected");

        assertThat(result.entries()).hasSize(2);
        assertThat(result.entries().get(1).event()).isEqualTo("violation_detected");
        verify(auditLedgerService).appendSafely("PJB_SLO_TIMELINE_QUERY", "SLO", "peticionamento", null, "event=violation_detected");
    }

    @Test
    void evaluate_deveRejeitarTempoInvalido() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        PjbSloRegistry sloRegistry = new PjbSloRegistry(meterRegistry);
        sloRegistry.registerSlos();
        PjbSloApplicationService applicationService = new PjbSloApplicationService(sloRegistry, mock(AuditLedgerService.class));

        assertThatThrownBy(() -> applicationService.evaluate(new PjbSloExecutionQuery("peticionamento", -1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("measuredSeconds invalido");
    }
}
